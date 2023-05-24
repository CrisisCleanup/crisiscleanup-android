package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.crisiscleanup.core.designsystem.R
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.disabledAlpha

@Composable
fun OutlinedSingleLineTextField(
    modifier: Modifier = Modifier,
    @StringRes
    labelResId: Int,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isError: Boolean,
    label: String = "",
    hasFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onNext: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    nextDirection: FocusDirection = FocusDirection.Down,
    readOnly: Boolean = false,
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
            focusManager.moveFocus(nextDirection)
            onNext?.invoke()
        },
        onSearch = {
            onSearch?.invoke()
        },
        onDone = {
            onEnter?.invoke()
        },
    )
    val labelText = if (labelResId == 0) label else stringResource(labelResId)
    val labelContent: (@Composable (() -> Unit)?) = if (labelText.isBlank()) null
    else {
        @Composable { Text(labelText) }
    }
    val trailingIconContent: (@Composable (() -> Unit)?) =
        if (value.isEmpty() || trailingIcon == null) null
        else {
            @Composable { trailingIcon() }
        }

    OutlinedTextField(
        modifier = modifier2,
        label = labelContent,
        value = value,
        // Physical keyboard input will append tab/enter characters. Use onscreen when testing.
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
        isError = isError,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIconContent,
        readOnly = readOnly,
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
    label: String = "",
    hasFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onNext: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    var tint = MaterialTheme.colorScheme.primary
    if (!enabled) {
        tint = tint.disabledAlpha()
    }
    val trailingIcon = @Composable {
        IconButton(
            onClick = { onValueChange("") },
            enabled = enabled,
        ) {
            Icon(
                CrisisCleanupIcons.Clear,
                contentDescription = stringResource(R.string.clear),
                tint = tint,
            )
        }
    }

    OutlinedSingleLineTextField(
        modifier = modifier,
        labelResId = labelResId,
        label = label,
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        isError = isError,
        hasFocus = hasFocus,
        keyboardType = keyboardType,
        onNext = onNext,
        onEnter = onEnter,
        onSearch = onSearch,
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