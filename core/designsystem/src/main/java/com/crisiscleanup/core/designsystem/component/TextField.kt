package com.crisiscleanup.core.designsystem.component

import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.crisiscleanup.core.designsystem.R
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

// TODO Refactor
@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun OutlinedSingleLineTextField(
    modifier: Modifier = Modifier,
    @StringRes
    labelResId: Int,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isError: Boolean,
    hasFocus: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    obfuscateValue: Boolean = false,
    isObfuscating: Boolean = false,
    onObfuscate: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
) {
    val focusRequester = FocusRequester()
    var modifier2 =
        if (hasFocus) modifier.then(Modifier.focusRequester(focusRequester)) else modifier

    val keyboardOptions2 = KeyboardOptions(
        imeAction = ImeAction.Done,
        keyboardType = keyboardOptions.keyboardType,
    )
    val focusManager = LocalFocusManager.current
    val keyboardActions = KeyboardActions(onDone = {
        focusManager.moveFocus(FocusDirection.Next)
        onEnter?.invoke()
    })
    modifier2 = modifier2.onKeyEvent {
        if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
            focusManager.moveFocus(FocusDirection.Next)
            onEnter?.invoke()
            return@onKeyEvent true
        }
        false
    }

    val visualTransformation =
        if (obfuscateValue && isObfuscating) PasswordVisualTransformation()
        else VisualTransformation.None

    OutlinedTextField(
        modifier = modifier2,
        label = { Text(stringResource(labelResId)) },
        value = value,
        // Physical keyboard input will append tab/enter characters. Use onscreen when testing.
        onValueChange = { onValueChange(it) },
        singleLine = true,
        keyboardOptions = keyboardOptions2,
        keyboardActions = keyboardActions,
        enabled = enabled,
        isError = isError,
        visualTransformation = visualTransformation,
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = {
                        if (obfuscateValue) {
                            onObfuscate?.invoke()
                        } else {
                            onValueChange("")
                        }
                    },
                    enabled = enabled,
                ) {
                    if (obfuscateValue) {
                        val icon = if (isObfuscating) CrisisCleanupIcons.Visibility
                        else CrisisCleanupIcons.VisibilityOff
                        val textResId = if (isObfuscating) R.string.show
                        else R.string.hide
                        Icon(
                            icon,
                            contentDescription = stringResource(textResId),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            CrisisCleanupIcons.Clear,
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    )

    if (hasFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}