package com.crisiscleanup.feature.authentication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.primaryBlueOneTenthColor


@Composable
fun SurvivorInfoRoute(
    enableBackHandler: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
) {
    SurvivorInfoScreen(
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurvivorInfoScreen(
    onBack: () -> Unit = {},
) {
    val t = LocalAppTranslator.current
    Column {
        TopAppBarBackAction(
            title = t("~~I need help"),
            onAction = onBack,
        )
        Column(
            modifier = Modifier
                .background(primaryBlueOneTenthColor)
                .padding(8.dp),
        ) {
            Text(
                modifier = listItemModifier,
                text = t("survivor.info_for_survivors"),
                style = LocalFontStyles.current.header1,
            )
            Text(
                modifier = listItemModifier,
                text = t("survivor.intro"),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}