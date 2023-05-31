package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
    drawOutline: Boolean = true,
) = SingleLineTextField(
    modifier,
    labelResId,
    value,
    onValueChange,
    enabled,
    isError,
    label,
    hasFocus,
    keyboardType,
    visualTransformation,
    onNext,
    onEnter,
    onSearch,
    trailingIcon,
    imeAction,
    nextDirection,
    readOnly,
    true,
)

@Composable
fun SingleLineTextField(
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
    drawOutline: Boolean = false,
    placeholder: String = "",
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
        { Text(labelText) }
    }
    val trailingIconContent: (@Composable (() -> Unit)?) =
        if (value.isEmpty() || trailingIcon == null) null
        else {
            { trailingIcon() }
        }
    val placeholderContent: (@Composable (() -> Unit)?) =
        if (placeholder.isBlank()) null
        else {
            { Text(placeholder) }
        }

    if (drawOutline) {
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
            placeholder = placeholderContent,
        )
    } else {
        TextField(
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
            placeholder = placeholderContent,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            )
        )
    }

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
) = ClearableTextField(
    modifier,
    labelResId,
    value,
    onValueChange,
    enabled,
    isError,
    label,
    hasFocus,
    keyboardType,
    onNext,
    onEnter,
    onSearch,
    imeAction,
    true,
)

@Composable
fun ClearableTextField(
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
    drawOutline: Boolean = false,
    placeholder: String = "",
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

    SingleLineTextField(
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
        drawOutline = drawOutline,
        placeholder = placeholder,
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

    SingleLineTextField(
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