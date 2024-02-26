package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Account
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AccountUpdateRepository
import com.crisiscleanup.core.model.data.InitiatePhoneLoginResult
import com.crisiscleanup.core.model.data.OrgData
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class LoginWithPhoneViewModel @Inject constructor(
    private val authApi: CrisisCleanupAuthApi,
    private val dataApi: CrisisCleanupNetworkDataSource,
    private val accountUpdateRepository: AccountUpdateRepository,
    private val accountDataRepository: AccountDataRepository,
    private val translator: KeyResourceTranslator,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(Account) private val logger: AppLogger,
) : ViewModel() {
    val uiState: StateFlow<AuthenticateScreenUiState> = accountDataRepository.accountData.map {
        AuthenticateScreenUiState.Ready(
            authenticationState = AuthenticationState(accountData = it),
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = AuthenticateScreenUiState.Loading,
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

    val singleCodes = mutableStateListOf("", "", "", "", "", "")

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
    val isSelectAccount = MutableStateFlow(true)
    val selectedAccount = MutableStateFlow(PhoneNumberAccountNone)

    /**
     * General error message during authentication
     */
    var errorMessage by mutableStateOf("")
        private set

    val isAuthenticateSuccessful = MutableStateFlow(false)

    fun onCloseScreen() {
        phoneNumberInput.value = ""
        errorMessage = ""
        isAuthenticateSuccessful.value = false
    }

    private fun resetVisualState() {
        errorMessage = ""
    }

    private fun clearAccountSelect() {
        isSelectAccount.value = false
        selectedAccount.value = PhoneNumberAccountNone
        accountOptions.clear()
    }

    fun onIncompleteCode() {
        errorMessage = translator("loginWithPhone.please_enter_full_phone")
    }

    fun requestPhoneCode(phoneNumber: String) {
        resetVisualState()
        clearAccountSelect()

        val trimPhoneNumber = phoneNumber.trim()
        if (numberRegex.matchEntire(trimPhoneNumber) == null) {
            errorMessage = translator("info.enter_valid_phone")
            return
        }

        if (isRequestingCode.value) {
            return
        }
        isRequestingCode.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                when (accountUpdateRepository.initiatePhoneLogin(trimPhoneNumber)) {
                    InitiatePhoneLoginResult.Success -> {
                        openPhoneCodeLogin = true
                    }

                    InitiatePhoneLoginResult.PhoneNotRegistered -> {
                        errorMessage =
                            translator("loginWithPhone.phone_number_not_registered")
                                .replace("phoneNumber", phoneNumber)
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

    fun authenticate(code: String) {
        val selectedUserId = selectedAccount.value.userId
        if (
            isSelectAccount.value &&
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
            isSelectAccount.value = true
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
                        oneTimePasswordId = result.otpId

                        // TODO Test associated accounts
                        accountOptions.clear()
                        if (result.associatedAccounts.size > 1) {
                            accountOptions.addAll(result.associatedAccounts)
                            selectedAccount.value = PhoneNumberAccountNone
                            isSelectAccount.value = true
                        } else {
                            selectedAccount.value = result.associatedAccounts.first()
                            isSelectAccount.value = false
                        }
                    }
                }

                var isSuccessful = false
                val accountId = selectedAccount.value.userId
                if (
                    accountId != 0L &&
                    oneTimePasswordId != 0L
                ) {
                    val accountData = accountDataRepository.accountData.first()
                    val otpAuth = authApi.oneTimePasswordLogin(accountId, oneTimePasswordId)
                    otpAuth?.let { tokens ->
                        if (
                            tokens.refreshToken?.isNotBlank() == true &&
                            tokens.accessToken?.isNotBlank() == true
                        ) {
                            dataApi.getProfile(tokens.accessToken!!)?.let { accountProfile ->
                                val emailAddress = accountData.emailAddress
                                if (emailAddress.isNotBlank() &&
                                    emailAddress != accountProfile.email
                                ) {
                                    errorMessage =
                                        translator("loginWithPhone.log_out_before_different_account")
                                    // TODO Clear account data and support logging in with different email address?
                                } else {
                                    val expirySeconds =
                                        Clock.System.now()
                                            .plus(tokens.expiresIn!!.seconds).epochSeconds
                                    accountDataRepository.setAccount(
                                        refreshToken = tokens.refreshToken!!,
                                        accessToken = tokens.accessToken!!,
                                        id = accountProfile.id,
                                        email = accountProfile.email,
                                        firstName = accountProfile.firstName,
                                        lastName = accountProfile.lastName,
                                        expirySeconds = expirySeconds,
                                        profilePictureUri = accountProfile.profilePicUrl ?: "",
                                        org = OrgData(
                                            id = accountProfile.organization.id,
                                            name = accountProfile.organization.name,
                                        ),
                                        hasAcceptedTerms = accountProfile.hasAcceptedTerms == true,
                                    )
                                    isSuccessful = true
                                }
                            }
                        }
                    }
                }

                if (!isSuccessful &&
                    errorMessage.isBlank()
                ) {
                    errorMessage = translator("loginWithPhone.login_failed_try_again")
                }

                isAuthenticateSuccessful.value = isSuccessful
            } catch (e: Exception) {
                // TODO Be more specific on the failure where possible
                errorMessage =
                    translator("loginWithPhone.check_number_try_again")
            } finally {
                isVerifyingCode.value = false
            }
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
