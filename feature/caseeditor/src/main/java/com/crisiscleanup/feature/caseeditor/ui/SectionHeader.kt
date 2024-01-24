package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CollapsibleIcon
import com.crisiscleanup.core.designsystem.component.HelpAction
import com.crisiscleanup.core.designsystem.component.WithHelpDialog
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.attentionBackgroundColor
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.feature.caseeditor.CreateEditCaseViewModel

@Composable
private fun CircleNumber(
    number: Int,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Box(
        Modifier
            .size(26.dp)
            .drawBehind {
                drawCircle(
                    color = attentionBackgroundColor,
                    radius = this.size.maxDimension * 0.5f,
                )
            },
    ) {
        Text(
            "$number",
            Modifier
                .testTag("circleNumberText_$number")
                .align(Alignment.Center),
            style = style,
            textAlign = TextAlign.Center,
        )
    }
}

private val headerTextStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalFontStyles.current.header3

@Composable
internal fun SectionHeaderCollapsible(
    viewModel: CreateEditCaseViewModel,
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
        if (help.isNotBlank()) {
            val translator = LocalAppTranslator.current
            val translateKey = "formLabels.$help"
            var translated = translator(translateKey)
            if (translated == translateKey) {
                translated = translator(help)
            }
            WithHelpDialog(viewModel, sectionTitle, translated, true) { showHelp ->
                HelpAction(viewModel.helpHint, showHelp)
            }
        }
        Spacer(Modifier.weight(1f))

        CollapsibleIcon(isCollapsed, sectionTitle)
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
        val sIndex = sectionIndex + 1
        CircleNumber(
            sIndex,
            style = textStyle,
        )
        Text(
            sectionTitle,
            Modifier
                .testTag("sectionHeaderTitle_${sIndex}_$sectionTitle")
                .listRowItemStartPadding(),
            style = textStyle,
        )
        trailingContent?.let {
            Spacer(Modifier.weight(1f))
            it()
        }
    }
}

@Preview
@Composable
private fun CircleNumberPreview() {
    CircleNumber(number = 1)
}
