package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.crisiscleanup.core.common.ParsedPhoneNumber
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.theme.listItemModifier

@Composable
fun PhoneCallDialog(
    parsedNumbers: List<ParsedPhoneNumber>,
    onCloseDialog: () -> Unit,
) {
    if (parsedNumbers.flatMap(ParsedPhoneNumber::parsedNumbers).isNotEmpty()) {
        CrisisCleanupAlertDialog(
            title = LocalAppTranslator.current("workType.phone"),
            onDismissRequest = onCloseDialog,
            confirmButton = {
                CrisisCleanupTextButton(
                    text = LocalAppTranslator.current("actions.close"),
                    onClick = onCloseDialog,
                    modifier = Modifier.testTag("phoneNumbersCloseAction"),
                )
            },
        ) {
            Column {
                for (parsedNumber in parsedNumbers) {
                    if (parsedNumber.parsedNumbers.isNotEmpty()) {
                        for (phoneNumber in parsedNumber.parsedNumbers) {
                            LinkifyPhoneText(
                                text = phoneNumber,
                                modifier = listItemModifier.testTag("phoneDialogLinkedText"),
                            )
                        }
                    } else {
                        Text(
                            parsedNumber.source,
                            modifier = listItemModifier.testTag("phoneDialogPhoneSourceText"),
                        )
                    }
                }
            }
        }
    }
}
