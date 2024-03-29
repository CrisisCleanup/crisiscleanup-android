package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.crisiscleanup.core.designsystem.LocalAppTranslator
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
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onNext: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    nextDirection: FocusDirection = FocusDirection.Down,
    readOnly: Boolean = false,
) = SingleLineTextField(
    modifier,
    value,
    onValueChange,
    enabled,
    isError,
    labelResId,
    label,
    hasFocus,
    keyboardType,
    keyboardCapitalization,
    visualTransformation,
    onNext,
    onEnter,
    onSearch,
    leadingIcon,
    trailingIcon,
    imeAction,
    nextDirection,
    readOnly,
    true,
)

@Composable
fun SingleLineTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isError: Boolean,
    @StringRes labelResId: Int = 0,
    label: String = "",
    hasFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onNext: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    nextDirection: FocusDirection = FocusDirection.Down,
    readOnly: Boolean = false,
    drawOutline: Boolean = false,
    placeholder: String = "",
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val focusRequester = FocusRequester()
    val modifier2 =
        if (hasFocus) modifier.then(Modifier.focusRequester(focusRequester)) else modifier

    val keyboardOptions = KeyboardOptions(
        imeAction = imeAction,
        keyboardType = keyboardType,
        capitalization = keyboardCapitalization,
    )
    val focusManager = LocalFocusManager.current
    val keyboardActions = KeyboardActions(
        onNext = {
            focusManager.moveFocus(nextDirection)
            onNext?.invoke()
        },
        onSearch = onSearch?.let { { onSearch() } },
        onDone = onEnter?.let { { onEnter() } },
    )
    val labelText = if (labelResId == 0) label else stringResource(labelResId)
    val labelContent: (@Composable (() -> Unit)?) = if (labelText.isBlank()) {
        null
    } else {
        { Text(labelText) }
    }
    val leadingIconContent: (@Composable (() -> Unit)?) =
        if (leadingIcon == null) {
            null
        } else {
            { leadingIcon() }
        }
    val trailingIconContent: (@Composable (() -> Unit)?) =
        if (value.isEmpty() || trailingIcon == null) {
            null
        } else {
            { trailingIcon() }
        }
    val placeholderContent: (@Composable (() -> Unit)?) =
        if (placeholder.isBlank()) {
            null
        } else {
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
            leadingIcon = leadingIconContent,
            trailingIcon = trailingIconContent,
            readOnly = readOnly,
            placeholder = placeholderContent,
            textStyle = textStyle,
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
            leadingIcon = leadingIconContent,
            trailingIcon = trailingIconContent,
            readOnly = readOnly,
            placeholder = placeholderContent,
            textStyle = textStyle,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
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
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isError: Boolean,
    @StringRes labelResId: Int = 0,
    label: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
    hasFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None,
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
    leadingIcon,
    hasFocus,
    keyboardType,
    keyboardCapitalization,
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
    leadingIcon: (@Composable () -> Unit)? = null,
    hasFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None,
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
                contentDescription = LocalAppTranslator.current("actions.clear"),
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
        keyboardCapitalization = keyboardCapitalization,
        onNext = onNext,
        onEnter = onEnter,
        onSearch = onSearch,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        imeAction = imeAction,
        drawOutline = drawOutline,
        placeholder = placeholder,
    )
}

@Composable
fun OutlinedObfuscatingTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isError: Boolean,
    @StringRes labelResId: Int = 0,
    label: String = "",
    hasFocus: Boolean = false,
    isObfuscating: Boolean = false,
    onObfuscate: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    val visualTransformation =
        if (isObfuscating) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        }

    val trailingIcon = @Composable {
        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onObfuscate?.invoke() },
                enabled = enabled,
            ) {
                val icon = if (isObfuscating) {
                    CrisisCleanupIcons.Visibility
                } else {
                    CrisisCleanupIcons.VisibilityOff
                }
                val translateKey = if (isObfuscating) {
                    "actions.show"
                } else {
                    "actions.hide"
                }
                val iconTestTag = if (isObfuscating) "textFieldShowIcon" else "textFieldHideIcon"
                val translator = LocalAppTranslator.current
                Icon(
                    icon,
                    contentDescription = translator(translateKey),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag(iconTestTag),
                )
            }
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
        keyboardType = KeyboardType.Password,
        visualTransformation = visualTransformation,
        onNext = onNext,
        onEnter = onEnter,
        trailingIcon = trailingIcon,
        imeAction = imeAction,
        drawOutline = true,
    )
}
