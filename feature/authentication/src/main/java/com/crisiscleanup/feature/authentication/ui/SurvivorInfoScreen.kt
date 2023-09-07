package com.crisiscleanup.feature.authentication.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.Accordion
import com.crisiscleanup.core.designsystem.component.LinkifyHtmlText
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.primaryBlueOneTenthColor
import com.crisiscleanup.feature.authentication.SurvivorInfoViewModel


@Composable
fun SurvivorInfoRoute(
    enableBackHandler: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
    viewModel: SurvivorInfoViewModel = hiltViewModel(),
) {
    SurvivorInfoScreen(
        onBack = onBack,
        viewModel = viewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurvivorInfoScreen(
    onBack: () -> Unit = {},
    viewModel: SurvivorInfoViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current
    val survivorInfo by viewModel.survivorInfoData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    Log.d("SURVIVOR", survivorInfo.toString())
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBarBackAction(
            title = t("~~I need help"),
            onAction = onBack,
        )
        if (isLoading) {
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        } else {
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
            Column {
                survivorInfo.forEach {
                    val title = translateContent(it.title, t)
                    val content = translateContent(it.content, t)
                    Log.d("SURVIVOR", "T: $title, C: $content")
                    Accordion(
                        title = title,
                        content = {
                            LinkifyHtmlText(
                                modifier = listItemModifier,
                                text = content,
                            )
                        },
                    )
                }
            }
        }
    }
}

fun translateContent(content: String, t: KeyResourceTranslator): String {
    val regex = "\\{(.+?)\\}".toRegex()
    return regex.replace(content) { matchResult ->
        val key = matchResult.groupValues[1]
        t(key)
    }
}
