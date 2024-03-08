package com.crisiscleanup.feature.authentication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.ListOptionsDropdown
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.OutlinedObfuscatingTextField
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.model.data.LanguageIdName
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.feature.authentication.model.UserInfoInputData

@Composable
private fun UserInfoErrorText(
    message: String,
    testTagSuffix: String,
) {
    if (message.isNotBlank()) {
        Text(
            message,
            Modifier
                .listItemHorizontalPadding()
                .listItemTopPadding()
                .testTag("userInfoError-$testTagSuffix"),
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
        UserInfoErrorText(infoData.emailAddressError, "email")
        OutlinedClearableTextField(
            modifier = listItemModifier.testTag("userInfoEmailTextField"),
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
        UserInfoErrorText(infoData.firstNameError, "firstName")
        OutlinedClearableTextField(
            modifier = listItemModifier.testTag("userInfoFirstNameTextField"),
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
        UserInfoErrorText(infoData.lastNameError, "lastName")
        OutlinedClearableTextField(
            modifier = listItemModifier.testTag("userInfoLastNameTextField"),
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
            modifier = listItemModifier.testTag("userInfoTitleTextField"),
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
        UserInfoErrorText(infoData.phoneError, "phone")
        OutlinedClearableTextField(
            modifier = listItemModifier.testTag("userInfoPhoneTextField"),
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
        UserInfoErrorText(infoData.passwordError, "password")
        OutlinedObfuscatingTextField(
            modifier = listItemModifier.testTag("userInfoPasswordTextField"),
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
        UserInfoErrorText(infoData.confirmPasswordError, "confirmPassword")
        OutlinedObfuscatingTextField(
            modifier = listItemModifier.testTag("userInfoConfirmPasswordTextField"),
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
            var showDropdown by remember { mutableStateOf(false) }
            val onToggleDropdown = remember(languageOptions) { { showDropdown = !showDropdown } }
            ListOptionsDropdown(
                selectedLanguage,
                isEditable,
                onToggleDropdown,
                Modifier.padding(16.dp),
                selectedLanguage,
            ) { contentSize ->
                if (languageOptions.isNotEmpty()) {
                    val onSelect = { language: LanguageIdName ->
                        infoData.language = language
                        showDropdown = false
                    }
                    DropdownMenu(
                        modifier = Modifier
                            .width(
                                with(LocalDensity.current) {
                                    contentSize.width
                                        .toDp()
                                        .minus(listItemDropdownMenuOffset.x.times(2))
                                },
                            )
                            .testTag("userInputLanguageOptions"),
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        offset = listItemDropdownMenuOffset,
                    ) {
                        DropdownLanguageItems(languageOptions, onSelect)
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
