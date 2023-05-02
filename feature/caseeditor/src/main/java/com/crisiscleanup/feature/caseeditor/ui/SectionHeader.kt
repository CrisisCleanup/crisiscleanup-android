package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
        .drawBehind {
            drawCircle(
                color = attentionBackgroundColor,
                radius = this.size.maxDimension * 0.5f,
            )
        },
    // TODO Bold
    style = style,
)

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
        // TODO Bold
        val textStyle = MaterialTheme.typography.bodyLarge
        // TODO Can surface and box be combined into a single element?
        Surface(
            // TODO Common dimensions
            Modifier.size(26.dp),
            shape = CircleShape,
            color = attentionBackgroundColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "${sectionIndex + 1}",
                    style = textStyle,
                )
            }
        }
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
    val textStyle = MaterialTheme.typography.bodyLarge
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
