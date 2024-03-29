package com.crisiscleanup.core.mapmarker.model

import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

private const val HALF_PI = Math.PI / 2
private const val TWO_PI = 2 * Math.PI
private const val FOUR_PI = 4 * Math.PI

// From https://stackoverflow.com/questions/23457916/how-to-get-latitude-and-longitude-bounds-from-google-maps-x-y-and-zoom-parameter
private fun yToLat(yNorm: Double) = Math.toDegrees(2 * atan(exp(TWO_PI * (0.5 - yNorm))) - HALF_PI)

data class TileCoordinates(
    val x: Int,
    val y: Int,
    val zoom: Int,
) {
    private val maxIndex: Int = 1 shl zoom
    private val southwest: LatLng
    private val northeast: LatLng
    private val lngRangeInverse: Double

    val querySouthwest: LatLng
    val queryNortheast: LatLng

    init {
        val maxInverse: Double = 1 / maxIndex.toDouble()

        val xWest = x * maxInverse - 0.5
        val xEast = (x + 1) * maxInverse - 0.5
        val lngWest = xWest * 360
        val lngEast = xEast * 360
        val lngRange = lngEast - lngWest
        lngRangeInverse = 1 / lngRange

        val ySouth = (y + 1) * maxInverse
        val yNorth = y * maxInverse

        // TODO Find values relative to dot size at the given zoom
        val padScale = if (zoom < 8) maxInverse * 32 else 1.0
        val padLongitude = padScale * 0.0004
        val padLatitude = padScale * 0.0001

        val latSouth = yToLat(ySouth)
        val latNorth = yToLat(yNorth)
        southwest = LatLng(latSouth, lngWest)
        northeast = LatLng(latNorth, lngEast)

        val querySouth = yToLat(ySouth + padLatitude)
        val queryWest = (xWest - padLongitude) * 360
        querySouthwest = LatLng(querySouth, queryWest)
        val queryNorth = yToLat(yNorth - padLatitude)
        val queryEast = (xEast + padLongitude) * 360
        queryNortheast = LatLng(queryNorth, queryEast)
    }

    /**
     * @return x (normalized longitude),y (normalized latitude) or null if coordinates are out of range
     */
    fun fromLatLng(
        latitude: Double,
        longitude: Double,
    ): Pair<Double, Double>? {
        if (latitude < querySouthwest.latitude ||
            latitude > queryNortheast.latitude ||
            longitude < querySouthwest.longitude ||
            longitude > queryNortheast.longitude
        ) {
            return null
        }

        val xNorm = (longitude - southwest.longitude) * lngRangeInverse

        // From https://developers.google.com/maps/documentation/javascript/examples/map-coordinates
        var siny = sin(Math.toRadians(latitude))
        // Truncating to 0.9999 effectively limits latitude to 89.189. This is
        // about a third of a tile past the edge of the world tile.
        siny = siny.coerceIn(-0.9999, 0.9999)
        var yNorm = 0.5 - ln((1 + siny) / (1 - siny)) / FOUR_PI
        yNorm = (yNorm * maxIndex) % 1

        if (latitude < southwest.latitude) {
            yNorm++
        } else if (latitude > northeast.latitude) {
            yNorm--
        }

        return Pair(xNorm, yNorm)
    }
}
