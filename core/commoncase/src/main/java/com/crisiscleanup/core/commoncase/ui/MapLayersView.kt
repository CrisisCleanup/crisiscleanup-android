package com.crisiscleanup.core.commoncase.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.roundedOutline
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLayersView(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    isSatelliteMapType: Boolean,
    onToggleSatelliteType: (Boolean) -> Unit,
) {
    if (isVisible) {
        val t = LocalAppTranslator.current

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            tonalElevation = 0.dp,
        ) {
            Column(
                listItemModifier,
                verticalArrangement = listItemSpacedByHalf,
            ) {
                Text(
                    t("worksiteMap.toggle_map_type"),
                    style = LocalFontStyles.current.header3,
                )

                val selectedOutline = Modifier.roundedOutline(
                    width = 3.dp,
                    color = primaryBlueColor,
                )

                Row(horizontalArrangement = listItemSpacedBy) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = listItemSpacedByHalf,
                    ) {
                        CrisisCleanupIconButton(
                            if (isSatelliteMapType) Modifier else selectedOutline,
                            imageVector = CrisisCleanupIcons.NormalMap,
                            onClick = { onToggleSatelliteType(false) },
                        )
                        Text(t("worksiteMap.street_map"))
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = listItemSpacedByHalf,
                    ) {
                        CrisisCleanupIconButton(
                            if (isSatelliteMapType) selectedOutline else Modifier,
                            imageVector = CrisisCleanupIcons.SatelliteMap,
                            onClick = { onToggleSatelliteType(true) },
                        )
                        Text(t("worksiteMap.satellite_map"))
                    }
                }
            }
        }
    }
}
