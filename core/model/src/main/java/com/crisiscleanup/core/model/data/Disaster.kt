package com.crisiscleanup.core.model.data

enum class Disaster(val literal: String) {
    ContaminatedWater("contaminated_water"),
    Earthquake("earthquake"),
    Fire("fire"),
    Flood("flood"),
    FloodRain("flood_rain"),
    FloodThunderStorm("flood_thunder"),
    Hail("hail"),
    Hurricane("hurricane"),
    MudSlide("mud_slide"),
    Other("other"),
    Snow("snow"),
    Tornado("tornado"),
    TornadoFlood("tornado_flood"),
    TornadoWindFlood("tornado_wind_flood"),
    TropicalStorm("tropical_storm"),
    Virus("virus"),
    Volcano("volcano"),
    Wind("wind"),
}

private val reverseLookup = Disaster.values().associateBy { it.literal }
fun disasterFromLiteral(literal: String) = reverseLookup[literal] ?: Disaster.Other
