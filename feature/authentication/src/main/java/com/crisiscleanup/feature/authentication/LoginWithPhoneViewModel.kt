package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.PhoneNumberPicker
import com.crisiscleanup.core.common.event.AccountEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Account
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AccountUpdateRepository
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.InitiatePhoneLoginResult
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.feature.authentication.model.AuthenticationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginWithPhoneViewModel @Inject constructor(
    private val authApi: CrisisCleanupAuthApi,
    private val dataApi: CrisisCleanupNetworkDataSource,
    private val accountUpdateRepository: AccountUpdateRepository,
    private val accountDataRepository: AccountDataRepository,
    private val phoneNumberPicker: PhoneNumberPicker,
    private val accountEventBus: AccountEventBus,
    private val translator: KeyResourceTranslator,
    appEnv: AppEnv,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(Account) private val logger: AppLogger,
) : ViewModel() {
    val viewState: StateFlow<AuthenticateScreenViewState> = accountDataRepository.accountData.map {
        AuthenticateScreenViewState.Ready(AuthenticationState(it))
    }.stateIn(
        scope = viewModelScope,
        initialValue = AuthenticateScreenViewState.Loading,
        started = SharingStarted.WhileSubscribed(),
    )

    val phoneNumberInput = MutableStateFlow("")
    private val numberRegex = """[\d -]+""".toRegex()
    private val nonNumberRegex = """\D""".toRegex()
    val phoneNumberNumbers = phoneNumberInput
        .map { it.replace(nonNumberRegex, "") }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )
    val obfuscatedPhoneNumber = phoneNumberNumbers
        .throttleLatest(250)
        .map {
            // TODO Refactor and test
            var s = it
            if (it.length > 4) {
                val startIndex = 0.coerceAtLeast(it.length - 4)
                val endIndex = it.length
                val lastFour = s.substring(startIndex, endIndex)
                val firstCount = s.length - 4
                fun obfuscated(count: Int): String {
                    return "•".repeat(count)
                }
                s = if (firstCount > 3) {
                    val obfuscatedStart = obfuscated(firstCount - 3)
                    val obfuscatedMiddle = obfuscated(3)
                    "($obfuscatedStart) $obfuscatedMiddle - $lastFour"
                } else {
                    val obfuscated = obfuscated(firstCount)
                    "$obfuscated - $lastFour"
                }
            }
            s
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    var phoneCode by mutableStateOf("")

    val isRequestingCode = MutableStateFlow(false)
    var openPhoneCodeLogin by mutableStateOf(false)

    private val isVerifyingCode = MutableStateFlow(false)

    val isExchangingCode = isRequestingCode.combine(
        isVerifyingCode,
        ::Pair,
    )
        .map { it.first || it.second }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private var oneTimePasswordId = 0L
    var accountOptions = mutableStateListOf<PhoneNumberAccount>()
    var isSelectAccount by mutableStateOf(false)
        private set
    val selectedAccount = MutableStateFlow(PhoneNumberAccountNone)

    /**
     * General error message during authentication
     */
    var errorMessage by mutableStateOf("")
        private set

    val isAuthenticateSuccessful = MutableStateFlow(false)

    // For UI testing
    val showMultiPhoneToggle = appEnv.isDebuggable

    init {
        phoneNumberPicker.phoneNumbers
            .onEach {
                if (it.isNotBlank() && phoneNumberInput.value.isBlank()) {
                    phoneNumberInput.value = it
                }
            }
            .launchIn(viewModelScope)
    }

    fun requestPhoneNumber() {
        if (phoneNumberInput.value.isBlank()) {
            phoneNumberPicker.requestPhoneNumber()
        }
    }

    fun onCloseScreen() {
        phoneNumberInput.value = ""
        errorMessage = ""
        isAuthenticateSuccessful.value = false
    }

    private fun resetVisualState() {
        errorMessage = ""
    }

    private fun clearAccountSelect() {
        isSelectAccount = false
        selectedAccount.value = PhoneNumberAccountNone
        accountOptions.clear()
    }

    fun onIncompleteCode() {
        errorMessage = translator("loginWithPhone.please_enter_full_code")
    }

    fun requestPhoneCode(phoneNumber: String) {
        resetVisualState()
        clearAccountSelect()
        oneTimePasswordId = 0

        val trimPhoneNumber = phoneNumber.trim()
        if (numberRegex.matchEntire(trimPhoneNumber) == null) {
            errorMessage = translator("info.enter_valid_phone")
            return
        }

        if (isRequestingCode.value) {
            return
        }
        isRequestingCode.value = true

        phoneCode = ""

        viewModelScope.launch(ioDispatcher) {
            try {
                when (accountUpdateRepository.initiatePhoneLogin(trimPhoneNumber)) {
                    InitiatePhoneLoginResult.Success -> {
                        openPhoneCodeLogin = true
                    }

                    InitiatePhoneLoginResult.PhoneNotRegistered -> {
                        errorMessage =
                            translator("loginWithPhone.phone_number_not_registered")
                                .replace("{phoneNumber}", phoneNumber)
                    }

                    else -> {
                        errorMessage =
                            translator("loginWithPhone.invalid_phone_unavailable_try_again")
                    }
                }
            } finally {
                isRequestingCode.value = false
            }
        }
    }

    private suspend fun verifyPhoneCode(phoneNumber: String, code: String): PhoneCodeVerification {
        val result = authApi.verifyPhoneCode(phoneNumber, code)
        val verification = result?.accounts?.map {
            PhoneNumberAccount(it.id, it.email, it.organizationName)
        }
            ?.let {
                return PhoneCodeVerification(
                    result.otpId!!,
                    it,
                    OneTimePasswordError.None,
                )
            }

        return verification ?: PhoneCodeVerification(
            0,
            emptyList(),
            OneTimePasswordError.InvalidCode,
        )
    }

    private suspend fun attemptAuthentication(
        accountId: Long,
        otpId: Long,
        accountData: AccountData,
    ): Pair<Boolean, String> {
        var authErrorMessage = ""
        var isSuccessful = false

        val otpAuth = authApi.oneTimePasswordLogin(accountId, otpId)
        otpAuth?.let { tokens ->
            if (
                tokens.refreshToken?.isNotBlank() == true &&
                tokens.accessToken?.isNotBlank() == true
            ) {
                val accessToken = tokens.accessToken!!
                dataApi.getProfile(accessToken)?.let { accountProfile ->
                    val emailAddress = accountData.emailAddress
                    if (emailAddress.isBlank() ||
                        emailAddress.lowercase() != accountProfile.email.lowercase()
                    ) {
                        authErrorMessage =
                            translator("loginWithPhone.log_out_before_different_account")
                        // TODO Clear account data and support logging in with different email address?
                    } else if (accountProfile.organization.isActive == false) {
                        accountEventBus.onAccountInactiveOrganization(accountId)
                    } else {
                        accountDataRepository.setAccount(
                            accountProfile,
                            refreshToken = tokens.refreshToken!!,
                            accessToken,
                            tokens.expiresIn!!,
                        )
                        isSuccessful = true
                    }
                }
            }
        }

        return Pair(isSuccessful, authErrorMessage)
    }

    fun authenticate(code: String) {
        val selectedUserId = selectedAccount.value.userId
        if (
            isSelectAccount &&
            selectedUserId == 0L
        ) {
            errorMessage = translator("loginWithPhone.select_account")
            return
        }

        if (
            accountOptions.isNotEmpty() &&
            accountOptions.find { it.userId == selectedUserId } == null
        ) {
            selectedAccount.value = PhoneNumberAccountNone
            isSelectAccount = true
            errorMessage = translator("loginWithPhone.select_account")
            return
        }

        if (isExchangingCode.value) {
            return
        }
        isVerifyingCode.value = true

        resetVisualState()

        viewModelScope.launch(ioDispatcher) {
            try {
                if (oneTimePasswordId == 0L) {
                    val result = verifyPhoneCode(phoneNumberInput.value, code)
                    if (result.associatedAccounts.isEmpty()) {
                        errorMessage = translator("loginWithPhone.no_account_error")
                        return@launch
                    } else {
                        errorMessage = ""

                        oneTimePasswordId = result.otpId

                        accountOptions.clear()
                        if (result.associatedAccounts.size > 1) {
                            accountOptions.addAll(result.associatedAccounts)
                            selectedAccount.value = PhoneNumberAccountNone
                            isSelectAccount = true
                        } else {
                            selectedAccount.value = result.associatedAccounts.first()
                            isSelectAccount = false
                        }
                    }
                }

                val accountId = selectedAccount.value.userId
                val accountData = accountDataRepository.accountData.first()
                val authResult = attemptAuthentication(
                    accountId,
                    otpId = oneTimePasswordId,
                    accountData,
                )
                val (isSuccessful, authErrorMessage) = authResult
                if (authErrorMessage.isNotBlank()) {
                    errorMessage = authErrorMessage
                }

                if (!isSuccessful &&
                    !isSelectAccount &&
                    errorMessage.isBlank()
                ) {
                    errorMessage = translator("loginWithPhone.login_failed_try_again")
                }

                isAuthenticateSuccessful.value = isSuccessful
            } catch (e: Exception) {
                // TODO Improve error messaging code structure
                //      There is a complex message when code length is different than expected
                val messageKey = if (e.message == "Invalid phone number or OTP.") {
                    "loginWithPhone.login_failed_try_again"
                } else {
                    "loginWithPhone.check_number_try_again"
                }
                // TODO Be more specific on the failure where possible
                errorMessage = translator(messageKey)
            } finally {
                isVerifyingCode.value = false
            }
        }
    }

    fun toggleMultiPhone() {
        if (showMultiPhoneToggle) {
            accountOptions.addAll(
                listOf(
                    PhoneNumberAccount(
                        7357_132621,
                        "Dev Stew",
                        "Hurris",
                    ),
                    PhoneNumberAccount(
                        7357_532958,
                        "Dev Hou",
                        "Tornas",
                    ),
                ),
            )
            selectedAccount.value = PhoneNumberAccountNone
            isSelectAccount = true
        }
    }
}

data class PhoneNumberAccount(
    val userId: Long,
    val userDisplayName: String,
    val organizationName: String,
    val accountDisplay: String = if (userId > 0) "$userDisplayName, $organizationName" else "",
)

val PhoneNumberAccountNone = PhoneNumberAccount(0, "", "")

private data class PhoneCodeVerification(
    val otpId: Long,
    val associatedAccounts: List<PhoneNumberAccount>,
    val error: OneTimePasswordError,
)

private enum class OneTimePasswordError {
    None,
    InvalidCode,
}
