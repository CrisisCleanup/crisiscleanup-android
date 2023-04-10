package com.crisiscleanup.feature.authentication

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.*
import com.crisiscleanup.core.network.model.NetworkAuthOrganization
import com.crisiscleanup.core.network.model.NetworkAuthResult
import com.crisiscleanup.core.network.model.NetworkAuthUserClaims
import com.crisiscleanup.core.network.retrofit.AuthApiClient
import com.crisiscleanup.core.testing.util.MainDispatcherRule
import com.crisiscleanup.feature.authentication.AuthenticateScreenUiState.Loading
import com.crisiscleanup.feature.authentication.AuthenticateScreenUiState.Ready
import com.crisiscleanup.feature.authentication.model.AuthenticationState
import com.crisiscleanup.feature.authentication.model.LoginInputData
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AuthenticationViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @MockK
    lateinit var accountDataRepository: AccountDataRepository

    @MockK
    lateinit var authApiClient: AuthApiClient

    @MockK
    lateinit var inputValidator: InputValidator

    @MockK
    lateinit var accessTokenDecoder: AccessTokenDecoder

    @MockK
    lateinit var authEventManager: AuthEventManager

    @MockK
    lateinit var appPreferences: LocalAppPreferencesRepository

    @MockK
    lateinit var appLogger: AppLogger

    @MockK
    lateinit var resProvider: AndroidResourceProvider

    private lateinit var viewModel: AuthenticationViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        coEvery {
            accountDataRepository.setAccount(
                id = any(),
                accessToken = any(),
                email = any(),
                firstName = any(),
                lastName = any(),
                expirySeconds = any(),
                profilePictureUri = any(),
                org = any(),
            )
        } returns Unit

        // TODO How to mock SaveCredentialsManager with coroutineScope=viewModelScope
        coEvery {
            appPreferences.userData
        } returns flowOf(
            UserData(
                darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                shouldHideOnboarding = false,
                saveCredentialsPromptCount = 0,
                disableSaveCredentialsPrompt = false,
                syncAttempt = SyncAttempt(0, 0, 0),
                selectedIncidentId = 0,
                languageKey = EnglishLanguage.key,
            )
        )

        every {
            authEventManager.addPasswordResultListener(any())
        } returns 1

        every { accessTokenDecoder.decode("access-token") } returns
                TestDecodedAccessToken(Clock.System.now().plus(864000L.seconds))

        every { resProvider.getString(any()) } returns "test-string"
    }

    private val emptyLoginData = LoginInputData()

    private val nonEmptyAccountData = AccountData(
        id = 19,
        "access-token",
        Clock.System.now().plus(1000.seconds),
        "display-name",
        "email-address",
        "profile-picture-uri",
        org = OrgData(813, "org"),
    )

    private fun buildViewModel() = AuthenticationViewModel(
        accountDataRepository,
        authApiClient,
        inputValidator,
        accessTokenDecoder,
        authEventManager,
        appPreferences,
        resProvider,
        UnconfinedTestDispatcher(),
        appLogger,
    )

    /**
     * View model starts out as not authenticating
     */
    @Test
    fun initialState() = runTest {
        // Setup
        val accountDataFlow = flow { emit(emptyAccountData) }
        every { accountDataRepository.accountData } returns accountDataFlow

        viewModel = buildViewModel()

        assertEquals(Loading, viewModel.uiState.value)
        assertTrue(viewModel.isNotAuthenticating.first())
    }

    @Test
    fun notAuthenticated_authenticateEmailPassword() = runTest {
        // Setup
        val accountDataFlow = flow { emit(emptyAccountData) }
        every { accountDataRepository.accountData } returns accountDataFlow

        every { inputValidator.validateEmailAddress(any()) } returns true

        viewModel = buildViewModel()

        // Initial state tests
        assertEquals(
            AuthenticationState(
                accountData = emptyAccountData,
                hasAccessToken = false,
            ), (viewModel.uiState.first() as Ready).authenticationState
        )
        assertEquals(emptyLoginData, viewModel.loginInputData)

        // Auth state has been accessed. View model is "loaded".
        assertNotEquals(Loading, viewModel.uiState.first())

        viewModel.loginInputData.apply {
            emailAddress = "email@address.com"
            password = "password"
        }

        // Authenticate

        coEvery { authApiClient.login("email@address.com", "password") } returns NetworkAuthResult(
            accessToken = "access-token",
            claims = NetworkAuthUserClaims(
                id = 534,
                email = "email@address.com",
                firstName = "first-name",
                lastName = "last-name",
                files = null,
            ),
            organizations = NetworkAuthOrganization(
                id = 813,
                name = "org",
                isActive = true,
            )
        )

        // TODO How to test state during authentication?
        viewModel.authenticateEmailPassword()

        val nowMillis = Clock.System.now().epochSeconds
        coVerify(exactly = 1) {
            accountDataRepository.setAccount(
                id = 534,
                accessToken = "access-token",
                email = "email@address.com",
                firstName = "first-name",
                lastName = "last-name",
                expirySeconds = match { millis -> abs(millis - nowMillis) < 8640000 },
                profilePictureUri = "",
                org = OrgData(813, "org"),
            )
        }

        assertTrue(viewModel.isNotAuthenticating.first())
        assertEquals(LoginInputData("email@address.com", "password"), viewModel.loginInputData)

        assertTrue(viewModel.errorMessage.value.isEmpty())
        assertFalse(viewModel.isInvalidEmail.value)
        assertFalse(viewModel.isInvalidPassword.value)
    }

    // TODO Other paths in authenticateEmailPassword()

    @Test
    fun authenticated_logout() = runTest {
        // Setup
        val accountDataFlow = flow { emit(nonEmptyAccountData) }
        every { accountDataRepository.accountData } returns accountDataFlow

        every { inputValidator.validateEmailAddress(any()) } returns true

        coEvery { authEventManager.onLogout() } returns Unit

        coEvery { authApiClient.logout() } returns Unit

        viewModel = buildViewModel()

        // Initial state tests
        assertEquals(
            AuthenticationState(
                accountData = nonEmptyAccountData,
                hasAccessToken = true,
            ), (viewModel.uiState.first() as Ready).authenticationState
        )

        assertEquals(LoginInputData("email-address"), viewModel.loginInputData)

        // Logout

        // TODO How to test state during authentication?
        viewModel.logout()

        coVerify(exactly = 1) { authApiClient.logout() }
        coVerify(exactly = 1) { authEventManager.onLogout() }

        assertEquals(LoginInputData(), viewModel.loginInputData)

        assertTrue(viewModel.errorMessage.value.isEmpty())
        assertFalse(viewModel.isInvalidEmail.value)
        assertFalse(viewModel.isInvalidPassword.value)
    }

    // TODO Other paths in logout()
}

class TestDecodedAccessToken(override val expiresAt: Instant) : DecodedAccessToken