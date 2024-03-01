package com.crisiscleanup.feature.authentication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.OutlinedObfuscatingTextField
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.component.roundedOutline
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.model.data.LanguageIdName
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.feature.authentication.model.UserInfoInputData

@Composable
private fun UserInfoErrorText(
    message: String,
) {
    if (message.isNotBlank()) {
        Text(
            message,
            Modifier
                .listItemHorizontalPadding()
                .listItemTopPadding(),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
internal fun UserInfoInputView(
    infoData: UserInfoInputData,
    languageOptions: List<LanguageIdName>,
    isEditable: Boolean,
    clearErrorVisuals: () -> Unit = {},
    onEndOfInput: () -> Unit = {},
) {
    val t = LocalAppTranslator.current
    val closeKeyboard = rememberCloseKeyboard(Unit)

    Column {
        // TODO Focus on top most error

        val hasEmailError = infoData.emailAddressError.isNotBlank()
        UserInfoErrorText(infoData.emailAddressError)
        OutlinedClearableTextField(
            modifier = listItemModifier,
            label = t("requestAccess.your_email"),
            value = infoData.emailAddress,
            onValueChange = { infoData.emailAddress = it },
            keyboardType = KeyboardType.Email,
            enabled = isEditable,
            isError = hasEmailError,
            hasFocus = hasEmailError,
            onNext = clearErrorVisuals,
        )

        val hasFirstNameError = infoData.firstNameError.isNotBlank()
        UserInfoErrorText(infoData.firstNameError)
        OutlinedClearableTextField(
            modifier = listItemModifier,
            label = t("invitationSignup.first_name_placeholder"),
            value = infoData.firstName,
            onValueChange = { infoData.firstName = it },
            keyboardType = KeyboardType.Text,
            keyboardCapitalization = KeyboardCapitalization.Words,
            enabled = isEditable,
            isError = hasFirstNameError,
            hasFocus = hasFirstNameError,
            onNext = clearErrorVisuals,
        )

        val hasLastNameError = infoData.lastNameError.isNotBlank()
        UserInfoErrorText(infoData.lastNameError)
        OutlinedClearableTextField(
            modifier = listItemModifier,
            label = t("invitationSignup.last_name_placeholder"),
            value = infoData.lastName,
            onValueChange = { infoData.lastName = it },
            keyboardType = KeyboardType.Text,
            keyboardCapitalization = KeyboardCapitalization.Words,
            enabled = isEditable,
            isError = hasLastNameError,
            hasFocus = hasLastNameError,
            onNext = clearErrorVisuals,
        )

        OutlinedClearableTextField(
            modifier = listItemModifier,
            label = t("invitationSignup.title_placeholder"),
            value = infoData.title,
            onValueChange = { infoData.title = it },
            keyboardType = KeyboardType.Text,
            keyboardCapitalization = KeyboardCapitalization.Words,
            enabled = isEditable,
            isError = false,
            hasFocus = false,
            onNext = clearErrorVisuals,
        )

        val hasPhoneError = infoData.phoneError.isNotBlank()
        UserInfoErrorText(infoData.phoneError)
        OutlinedClearableTextField(
            modifier = listItemModifier,
            label = t("invitationSignup.mobile_placeholder"),
            value = infoData.phone,
            onValueChange = { infoData.phone = it },
            keyboardType = KeyboardType.Password,
            enabled = isEditable,
            isError = hasPhoneError,
            hasFocus = hasPhoneError,
            onNext = clearErrorVisuals,
        )

        var isObfuscatingPassword by rememberSaveable { mutableStateOf(true) }
        val hasPasswordError = infoData.passwordError.isNotBlank()
        UserInfoErrorText(infoData.passwordError)
        OutlinedObfuscatingTextField(
            modifier = listItemModifier,
            label = t("invitationSignup.pw1_placeholder"),
            value = infoData.password,
            onValueChange = { infoData.password = it },
            isObfuscating = isObfuscatingPassword,
            onObfuscate = { isObfuscatingPassword = !isObfuscatingPassword },
            enabled = isEditable,
            isError = hasPasswordError,
            hasFocus = hasPasswordError,
            onNext = clearErrorVisuals,
        )

        var isObfuscatingConfirmPassword by rememberSaveable { mutableStateOf(true) }
        val hasConfirmPasswordError = infoData.confirmPasswordError.isNotBlank()
        UserInfoErrorText(infoData.confirmPasswordError)
        OutlinedObfuscatingTextField(
            modifier = listItemModifier,
            label = t("invitationSignup.pw2_placeholder"),
            value = infoData.confirmPassword,
            onValueChange = { infoData.confirmPassword = it },
            isObfuscating = isObfuscatingConfirmPassword,
            onObfuscate = { isObfuscatingConfirmPassword = !isObfuscatingConfirmPassword },
            enabled = isEditable,
            isError = hasConfirmPasswordError,
            hasFocus = hasConfirmPasswordError,
            onEnter = {
                clearErrorVisuals()
                closeKeyboard()
                onEndOfInput()
            },
            imeAction = ImeAction.Done,
        )

        val selectedLanguage = t(infoData.language.name.ifBlank { "languages.en-us" })
        Box(Modifier.listItemBottomPadding()) {
            var contentSize by remember { mutableStateOf(Size.Zero) }
            var showDropdown by remember { mutableStateOf(false) }
            Row(
                Modifier
                    .padding(16.dp)
                    .actionHeight()
                    .roundedOutline(radius = 3.dp)
                    .clickable(
                        onClick = { showDropdown = !showDropdown },
                        enabled = isEditable,
                    )
                    .listItemPadding()
                    .onGloballyPositioned {
                        contentSize = it.size.toSize()
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selectedLanguage)
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = CrisisCleanupIcons.ExpandAll,
                    contentDescription = selectedLanguage,
                )
            }

            if (languageOptions.isNotEmpty()) {
                val onSelect = { language: LanguageIdName ->
                    infoData.language = language
                    showDropdown = false
                }
                DropdownMenu(
                    modifier = Modifier
                        .width(
                            with(LocalDensity.current) {
                                contentSize.width.toDp()
                                    .minus(listItemDropdownMenuOffset.x.times(2))
                            },
                        ),
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    offset = listItemDropdownMenuOffset,
                ) {
                    DropdownLanguageItems(
                        languageOptions,
                    ) {
                        onSelect(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownLanguageItems(
    languages: List<LanguageIdName>,
    onSelect: (LanguageIdName) -> Unit,
) {
    for (language in languages) {
        key(language.id) {
            DropdownMenuItem(
                modifier = Modifier.optionItemHeight(),
                text = {
                    Text(
                        LocalAppTranslator.current(language.name),
                        style = LocalFontStyles.current.header4,
                    )
                },
                onClick = { onSelect(language) },
            )
        }
    }
}
