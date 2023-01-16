package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.crisiscleanup.core.designsystem.R
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

@OptIn(
    ExperimentalMaterial3Api::class,
)
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
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    obfuscateValue: Boolean = false,
) {
    val focusRequester = FocusRequester()
    var modifier2 =
        if (hasFocus) modifier.then(Modifier.focusRequester(focusRequester)) else modifier

    OutlinedTextField(
        modifier = modifier2,
        label = { Text(stringResource(labelResId)) },
        value = value,
        onValueChange = { onValueChange(it) },
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        isError = isError,
        visualTransformation = if (obfuscateValue) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    enabled = enabled,
                ) {
                    Icon(
                        CrisisCleanupIcons.Clear,
                        contentDescription = stringResource(id = R.string.clear),
                        tint = MaterialTheme.colorScheme.primary,
                    )
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