package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CardSurface
import com.crisiscleanup.core.designsystem.component.LinkifyEmailText
import com.crisiscleanup.core.designsystem.component.LinkifyPhoneText
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.neutralBackgroundColor
import com.crisiscleanup.core.designsystem.theme.neutralFontColor
import com.crisiscleanup.core.model.data.CaseHistoryEvent
import com.crisiscleanup.core.model.data.CaseHistoryUserEvents
import com.crisiscleanup.feature.caseeditor.CaseHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseEditCaseHistoryRoute(
    onBack: () -> Unit = {},
    viewModel: CaseHistoryViewModel = hiltViewModel(),
) {
    val translator = viewModel.translator
    CompositionLocalProvider(
        LocalAppTranslator provides translator,
    ) {
        val isLoadingCaseHistory by viewModel.isLoadingCaseHistory.collectAsStateWithLifecycle(false)

        val historyEvents by viewModel.historyEvents.collectAsStateWithLifecycle()
        val hasEvents by viewModel.hasEvents.collectAsStateWithLifecycle()

        Column(Modifier.fillMaxSize()) {
            TopAppBarBackAction(
                title = viewModel.screenTitle,
                onAction = onBack,
            )

            Text(
                translator("caseHistory.do_not_share_contact_warning"),
                modifier = listItemModifier,
                style = LocalFontStyles.current.header3,
            )

            Box(Modifier.weight(1f)) {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                ) {
                    item {
                        Text(
                            translator("caseHistory.do_not_share_contact_explanation"),
                            listItemModifier,
                        )
                    }

                    if (hasEvents) {
                        items(
                            historyEvents,
                            key = { it.userId }) {
                            CardSurface(listItemModifier) {
                                Column(Modifier.fillMaxWidth()) {
                                    HistoryUser(it)
                                    HistoryEvents(it.events)
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                "~~No history",
                                listItemModifier,
                            )
                        }
                    }
                }

                BusyIndicatorFloatingTopCenter(isBusy = isLoadingCaseHistory)
            }
        }
    }
}

@Composable
private fun HistoryUser(
    userInfo: CaseHistoryUserEvents,
) {
    Column(
        Modifier
            .background(Color.White)
            // TODO Common dimensions
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = listItemSpacedBy) {
            Text(
                userInfo.userName,
                Modifier.weight(1f),
                style = LocalFontStyles.current.header4,
            )
            LinkifyPhoneText(userInfo.userPhone)
        }
        Row(horizontalArrangement = listItemSpacedBy) {
            Text(
                userInfo.orgName,
                Modifier.weight(1f),
            )
            if (userInfo.userEmail.isNotBlank()) {
                LinkifyEmailText(userInfo.userEmail)
            }
        }
    }
}

@Composable
private fun HistoryEvents(
    events: List<CaseHistoryEvent>,
) {
    Column(
        Modifier
            .background(neutralBackgroundColor)
            // TODO Common dimensions
            .padding(16.dp),
        verticalArrangement = listItemSpacedBy
    ) {
        for (event in events) {
            Column {
                Text(event.pastTenseDescription)
                Row(
                    Modifier
                        .fillMaxWidth()
                        // TODO Common dimensions
                        .padding(top = 4.dp),
                    horizontalArrangement = listItemSpacedBy,
                ) {
                    Text(
                        event.createdAt.relativeTime,
                        color = neutralFontColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        event.actorLocationName,
                        color = neutralFontColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}