package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.attentionBackgroundColor
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.feature.caseeditor.CaseEditorViewModel
import com.crisiscleanup.feature.caseeditor.R

@Composable
private fun CircleNumber(
    number: Int,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) = Text(
    "$number",
    modifier = Modifier
        // TODO Common dimensions
        .size(26.dp)
        .padding(top = 2.dp)
        .drawBehind {
            drawCircle(
                color = attentionBackgroundColor,
                radius = this.size.maxDimension * 0.5f,
            )
        },
    style = style,
    textAlign = TextAlign.Center,
)

// TODO Common styles
private val headerTextStyle: TextStyle
    @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)

@Composable
internal fun SectionHeaderCollapsible(
    viewModel: CaseEditorViewModel,
    modifier: Modifier = Modifier,
    sectionIndex: Int,
    sectionTitle: String,
    isCollapsed: Boolean = false,
    toggleCollapse: () -> Unit = {},
    help: String = "",
) {
    Row(
        modifier
            .clickable(onClick = toggleCollapse)
            .listItemHeight()
            .listItemPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val textStyle = headerTextStyle
        CircleNumber(
            sectionIndex + 1,
            style = textStyle,
        )
        Text(
            sectionTitle,
            Modifier.listRowItemStartPadding(),
            style = textStyle,
        )
        val iconVector =
            if (isCollapsed) CrisisCleanupIcons.ExpandLess else CrisisCleanupIcons.ExpandMore
        val descriptionResId =
            if (isCollapsed) R.string.collapse_section else R.string.expand_section
        val description = stringResource(descriptionResId, sectionTitle)
        if (help.isNotBlank()) {
            WithHelpDialog(viewModel, sectionTitle, help, true) { showHelp ->
                HelpAction(viewModel.helpHint, showHelp)
            }
        }
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = iconVector,
            contentDescription = description,
        )
    }
}

@Composable
internal fun SectionHeader(
    modifier: Modifier = Modifier,
    sectionIndex: Int,
    sectionTitle: String,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val textStyle = headerTextStyle
    Row(
        modifier
            .listItemHeight()
            .listItemPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleNumber(
            sectionIndex + 1,
            style = textStyle,
        )
        Text(
            sectionTitle,
            Modifier.listRowItemStartPadding(),
            style = textStyle,
        )
        trailingContent?.let {
            Spacer(Modifier.weight(1f))
            it()
        }
    }
}
