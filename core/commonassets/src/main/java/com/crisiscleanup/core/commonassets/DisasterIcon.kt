package com.crisiscleanup.core.commonassets

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContainerColor
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContentColor
import com.crisiscleanup.core.model.data.Disaster

private val statusIcons = mapOf(
    Disaster.Hurricane to R.drawable.ic_hurricane,
    Disaster.ContaminatedWater to R.drawable.ic_contaminated_water,
    Disaster.Earthquake to R.drawable.ic_earthquake,
    Disaster.Fire to R.drawable.ic_fire,
    Disaster.Flood to R.drawable.ic_flood,
    Disaster.FloodRain to R.drawable.ic_flood_rain,
    Disaster.FloodThunderStorm to R.drawable.ic_flood_thunder,
    Disaster.Hail to R.drawable.ic_hail,
    Disaster.Hurricane to R.drawable.ic_hurricane,
    Disaster.MudSlide to R.drawable.ic_mudslide,
    Disaster.Other to R.drawable.ic_disaster_other,
    Disaster.Snow to R.drawable.ic_snow,
    Disaster.Tornado to R.drawable.ic_tornado,
    Disaster.TornadoFlood to R.drawable.ic_tornado_flood,
    Disaster.TornadoWindFlood to R.drawable.ic_tornado_wind_flood,
    Disaster.TropicalStorm to R.drawable.ic_tropical_storm,
    Disaster.Virus to R.drawable.ic_virus,
    Disaster.Volcano to R.drawable.ic_volcano,
    Disaster.Wind to R.drawable.ic_wind,
)

fun getDisasterIcon(disaster: Disaster) = statusIcons[disaster] ?: R.drawable.ic_disaster_other

@Composable
fun DisasterIcon(
    @DrawableRes disasterResId: Int,
    incidentName: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = incidentDisasterContainerColor,
        contentColor = incidentDisasterContentColor,
    ) {
        Icon(
            modifier = Modifier.padding(2.dp),
            painter = painterResource(disasterResId),
            contentDescription = incidentName,
        )
    }
}
