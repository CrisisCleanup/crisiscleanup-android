package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SelectableChipBorder
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.disabledAlpha

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectableFilterChip(
    selected: Boolean,
    onClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    chipBorder: SelectableChipBorder = ChipDefaults.filterChipBorder(),
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
    border = chipBorder,
    colors = chipColors,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisCleanupFilterChip(
    selected: Boolean,
    onClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
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
    const val DisabledChipContainerAlpha = 0.12f
    val ChipBorderWidth = 1.dp

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun filterChipBorder(): SelectableChipBorder {
        val colors = MaterialTheme.colorScheme
        return FilterChipDefaults.filterChipBorder(
            borderColor = colors.onBackground,
            selectedBorderColor = colors.primaryContainer,
            disabledBorderColor = colors.onBackground.disabledAlpha(),
            disabledSelectedBorderColor = colors.onBackground.disabledAlpha(),
            selectedBorderWidth = ChipBorderWidth,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun filterChipColors(selected: Boolean): SelectableChipColors {
        val colors = MaterialTheme.colorScheme
        return FilterChipDefaults.filterChipColors(
            labelColor = colors.onBackground,
            iconColor = colors.onBackground,
            disabledContainerColor = if (selected) {
                colors.onBackground.copy(
                    alpha = DisabledChipContainerAlpha,
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
