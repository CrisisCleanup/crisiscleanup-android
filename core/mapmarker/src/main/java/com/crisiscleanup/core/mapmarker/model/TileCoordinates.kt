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
    val southwest: LatLng
    val northeast: LatLng
    val boundsPadding: Double
    private val lngRangeInverse: Double

    init {
        val maxInverse: Double = 1 / maxIndex.toDouble()

        val lngWest = (x * maxInverse - 0.5) * 360
        val lngEast = ((x + 1) * maxInverse - 0.5) * 360
        val lngRange = lngEast - lngWest
        lngRangeInverse = 1 / lngRange

        val ySouth = (y + 1) * maxInverse
        val yNorth = y * maxInverse

        // TODO This is not tested and doesn't seem to be working as designed.
        //      Is it possible to be more exact based on the zoom level?
        boundsPadding = maxInverse / 4

        southwest = LatLng(yToLat(ySouth), lngWest)
        northeast = LatLng(yToLat(yNorth), lngEast)
    }

    /**
     * @return x (normalized longitude),y (normalized latitude) or null if coordinates are out of range
     */
    fun fromLatLng(
        latitude: Double,
        longitude: Double,
    ): Pair<Double, Double>? {
        if (latitude < southwest.latitude || latitude > northeast.latitude ||
            longitude < southwest.longitude || longitude > northeast.longitude
        ) {
            return null
        }

        val xNorm = (longitude - southwest.longitude) * lngRangeInverse

        // From https://developers.google.com/maps/documentation/javascript/examples/map-coordinates
        var siny = sin(Math.toRadians(latitude))
        // Truncating to 0.9999 effectively limits latitude to 89.189. This is
        // about a third of a tile past the edge of the world tile.
        siny = siny.coerceAtLeast(-0.9999).coerceAtMost(0.9999)
        var yNorm = 0.5 - ln((1 + siny) / (1 - siny)) / FOUR_PI
        yNorm = (yNorm * maxIndex) % 1

        return Pair(xNorm, yNorm)
    }
}
