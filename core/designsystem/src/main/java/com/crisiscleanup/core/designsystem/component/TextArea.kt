package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.crisiscleanup.core.designsystem.theme.textBoxHeight

@Composable
fun CrisisCleanupTextArea(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit = {},
    placeholder: (@Composable () -> Unit)? = null,
    nextDirection: FocusDirection = FocusDirection.Down,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    hasFocus: Boolean = false,
    enabled: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
) {
    val focusRequester = FocusRequester()

    val keyboardOptions = KeyboardOptions(
        imeAction = imeAction,
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Sentences,
    )
    val focusManager = LocalFocusManager.current
    val keyboardActions = KeyboardActions(
        onNext = {
            focusManager.moveFocus(nextDirection)
            onNext?.invoke()
        },
        onDone = onDone?.let { { onDone() } }
    )
    OutlinedTextField(
        text,
        { value: String -> onTextChange(value) },
        modifier = modifier
            .textBoxHeight()
            .focusRequester(focusRequester),
        label = label,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
    )
    if (hasFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
