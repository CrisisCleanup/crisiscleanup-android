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
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onNext: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    val focusRequester = FocusRequester()
    val modifier2 =
        if (hasFocus) modifier.then(Modifier.focusRequester(focusRequester)) else modifier

    val keyboardOptions = KeyboardOptions(
        imeAction = imeAction,
        keyboardType = keyboardType,
    )
    val focusManager = LocalFocusManager.current
    val keyboardActions = KeyboardActions(
        onNext = {
            focusManager.moveFocus(FocusDirection.Next)
            onNext?.invoke()
        },
        onDone = {
            onEnter?.invoke()
        },
    )

    OutlinedTextField(
        modifier = modifier2,
        label = { Text(stringResource(labelResId)) },
        value = value,
        // Physical keyboard input will append tab/enter characters. Use onscreen when testing.
        onValueChange = { onValueChange(it) },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
        isError = isError,
        visualTransformation = visualTransformation,
        trailingIcon = {
            if (value.isNotEmpty() && trailingIcon != null) {
                trailingIcon()
            }
        },
    )

    if (hasFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun OutlinedClearableTextField(
    modifier: Modifier = Modifier,
    @StringRes
    labelResId: Int,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isError: Boolean,
    hasFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onNext: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    val trailingIcon = @Composable {
        IconButton(
            onClick = { onValueChange("") },
            enabled = enabled,
        ) {
            Icon(
                CrisisCleanupIcons.Clear,
                contentDescription = stringResource(R.string.clear),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }

    OutlinedSingleLineTextField(
        modifier = modifier,
        labelResId = labelResId,
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        isError = isError,
        hasFocus = hasFocus,
        keyboardType = keyboardType,
        onNext = onNext,
        onEnter = onEnter,
        trailingIcon = trailingIcon,
        imeAction = imeAction,
    )
}

@Composable
fun OutlinedObfuscatingTextField(
    modifier: Modifier = Modifier,
    @StringRes
    labelResId: Int,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isError: Boolean,
    hasFocus: Boolean = false,
    isObfuscating: Boolean = false,
    onObfuscate: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    val visualTransformation =
        if (isObfuscating) PasswordVisualTransformation()
        else VisualTransformation.None

    val trailingIcon = @Composable {
        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onObfuscate?.invoke() },
                enabled = enabled,
            ) {
                val icon = if (isObfuscating) CrisisCleanupIcons.Visibility
                else CrisisCleanupIcons.VisibilityOff
                val textResId = if (isObfuscating) R.string.show
                else R.string.hide
                Icon(
                    icon,
                    contentDescription = stringResource(textResId),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    OutlinedSingleLineTextField(
        modifier = modifier,
        labelResId = labelResId,
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        isError = isError,
        hasFocus = hasFocus,
        keyboardType = KeyboardType.Password,
        visualTransformation = visualTransformation,
        onNext = onNext,
        onEnter = onEnter,
        trailingIcon = trailingIcon,
        imeAction = imeAction,
    )
}