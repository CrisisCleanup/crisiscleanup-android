package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.crisiscleanup.core.designsystem.theme.disabledAlpha

@Composable
fun SelectableFilterChip(
    selected: Boolean,
    onClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    chipColors: SelectableChipColors = ChipDefaults.filterChipColors(selected),
) = FilterChip(
    selected = selected,
    onClick = { onClick(!selected) },
    label = {
        ProvideTextStyle(value = textStyle) {
            label()
        }
    },
    modifier = modifier,
    enabled = enabled,
    leadingIcon = leadingIcon,
    shape = CircleShape,
    colors = chipColors,
)

@Composable
fun CrisisCleanupFilterChip(
    selected: Boolean,
    onClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
) = SelectableFilterChip(
    selected,
    onClick,
    modifier,
    enabled,
    leadingIcon,
    label,
    textStyle,
)

private object ChipDefaults {
    // TODO: File bug
    // FilterChip default values aren't exposed via FilterChipDefaults
    const val DISABLED_CHIP_CONTAINER_ALPHA = 0.12f

    @Composable
    fun filterChipColors(selected: Boolean): SelectableChipColors {
        val colors = MaterialTheme.colorScheme
        return FilterChipDefaults.filterChipColors(
            labelColor = colors.onBackground,
            iconColor = colors.onBackground,
            disabledContainerColor = if (selected) {
                colors.onBackground.copy(
                    alpha = DISABLED_CHIP_CONTAINER_ALPHA,
                )
            } else {
                Color.Transparent
            },
            disabledLabelColor = colors.onBackground.disabledAlpha(),
            disabledLeadingIconColor = colors.onBackground.disabledAlpha(),
            selectedContainerColor = colors.primaryContainer,
            selectedLabelColor = colors.onBackground,
            selectedLeadingIconColor = colors.onBackground,
        )
    }
}
