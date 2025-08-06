package com.crisiscleanup.feature.authentication

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppSettingsProvider
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.event.AccountEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AppPreferencesRepository
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.model.data.emptyAccountData
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkOrganizationShort
import com.crisiscleanup.core.network.model.NetworkUserProfile
import com.crisiscleanup.core.testing.model.UserDataNone
import com.crisiscleanup.core.testing.util.MainDispatcherRule
import com.crisiscleanup.feature.authentication.AuthenticateScreenViewState.Loading
import com.crisiscleanup.feature.authentication.AuthenticateScreenViewState.Ready
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class AuthenticationViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @MockK
    lateinit var accountDataRepository: AccountDataRepository

    @MockK
    lateinit var authApiClient: CrisisCleanupAuthApi

    @MockK
    lateinit var dataApiClient: CrisisCleanupNetworkDataSource

    @MockK
    lateinit var inputValidator: InputValidator

    @MockK
    lateinit var accessTokenDecoder: AccessTokenDecoder

    @MockK
    lateinit var accountEventBus: AccountEventBus

    @MockK
    lateinit var appPreferences: AppPreferencesRepository

    @MockK
    lateinit var translator: KeyResourceTranslator

    @MockK
    lateinit var appLogger: AppLogger

    @MockK
    lateinit var resProvider: AndroidResourceProvider

    @MockK
    lateinit var settingsProvider: AppSettingsProvider

    private lateinit var viewModel: AuthenticationViewModel

    // private val passwordCredentialsStream = MutableSharedFlow<PasswordCredentials>(0)

    private val testAppEnv = object : AppEnv {
        override val isDebuggable = false
        override val isProduction = true
        override val isNotProduction = false
        override val isEarlybird = false
        override val apiEnvironment = ""

        override fun runInNonProd(block: () -> Unit) {
        }
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        coEvery {
            accountDataRepository.setAccount(
                id = any(),
                accessToken = any(),
                email = any(),
                phone = any(),
                firstName = any(),
                lastName = any(),
                expirySeconds = any(),
                profilePictureUri = any(),
                org = any(),
                hasAcceptedTerms = any(),
                approvedIncidentIds = any(),
                refreshToken = any(),
                activeRoles = any(),
            )
        } returns Unit

        // TODO How to mock SaveCredentialsManager with coroutineScope=viewModelScope
        coEvery {
            appPreferences.preferences
        } returns flowOf(UserDataNone)

        // every {
        //     accountEventBus.passwordCredentialResults
        // } returns passwordCredentialsStream

        every {
            accessTokenDecoder.decode("access-token")
        } returns DecodedAccessToken(Clock.System.now().plus(864000L.seconds))

        every {
            resProvider.getString(any())
        } returns "test-string"

        // TODO Configure or delete
//        every {
//            settingsProvider.
//        }
    }

    private val emptyLoginData = LoginInputData()

    private val nonEmptyAccountData = AccountData(
        id = 19,
        Clock.System.now().plus(1000.seconds),
        fullName = "display-name",
        emailAddress = "email-address",
        profilePictureUri = "profile-picture-uri",
        org = OrgData(813, "org"),
        hasAcceptedTerms = false,
        approvedIncidents = setOf(153),
        areTokensValid = false,
        isCrisisCleanupAdmin = false,
    )

    private fun buildViewModel() = AuthenticationViewModel(
        accountDataRepository,
        authApiClient,
        dataApiClient,
        inputValidator,
        accountEventBus,
        translator,
        testAppEnv,
        settingsProvider,
        UnconfinedTestDispatcher(),
        appLogger,
    )

    /**
     * View model starts out as not authenticating
     */
    @Test
    @Ignore("Auth flow will change. Ignoring this test for now...")
    fun initialState() = runTest {
        // Setup
        val accountDataFlow = flow { emit(emptyAccountData) }
        every { accountDataRepository.accountData } returns accountDataFlow

        viewModel = buildViewModel()

        assertEquals(Loading, viewModel.viewState.value)
        assertTrue(viewModel.isNotAuthenticating.first())
    }

    @Test
    @Ignore("Auth flow will change. Ignoring this test for now...")
    fun notAuthenticated_authenticateEmailPassword() = runTest {
        // Setup
        val accountDataFlow = flow { emit(emptyAccountData) }
        every { accountDataRepository.accountData } returns accountDataFlow

        every { inputValidator.validateEmailAddress(any()) } returns true

        coEvery {
            accountDataRepository.isAuthenticated
        } returns flowOf(true)

        viewModel = buildViewModel()

        // Initial state tests
        assertEquals(
            AuthenticationState(
                accountData = emptyAccountData,
            ),
            (viewModel.viewState.first() as Ready).authenticationState,
        )
        assertEquals(emptyLoginData, viewModel.loginInputData)

        // Auth state has been accessed. View model is "loaded".
        assertNotEquals(Loading, viewModel.viewState.first())

        viewModel.loginInputData.apply {
            emailAddress = "email@address.com"
            password = "password"
        }

        // Authenticate

        coEvery { dataApiClient.getProfile("access-token") } returns NetworkUserProfile(
            id = 534,
            email = "email@address.com",
            mobile = "9876543210",
            firstName = "first-name",
            lastName = "last-name",
            approvedIncidents = setOf(53),
            hasAcceptedTerms = true,
            acceptedTermsTimestamp = Clock.System.now().minus(1.days),
            files = null,
            activeRoles = emptySet(),
            organization = NetworkOrganizationShort(
                id = 813,
                name = "org",
                isActive = true,
            ),
        )

        // TODO How to test state during authentication?
        viewModel.authenticateEmailPassword()

        val nowSeconds = Clock.System.now().epochSeconds
        coVerify(exactly = 1) {
            accountDataRepository.setAccount(
                id = 534,
                refreshToken = "refresh-token",
                accessToken = "access-token",
                email = "email@address.com",
                phone = "9876543210",
                firstName = "first-name",
                lastName = "last-name",
                expirySeconds = match { seconds -> abs(seconds - nowSeconds) < 864000L + 1000 },
                profilePictureUri = "",
                org = OrgData(813, "org"),
                hasAcceptedTerms = true,
                approvedIncidentIds = setOf(53),
                activeRoles = emptySet(),
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
    @Ignore("Auth flow will change. Ignoring this test for now...")
    fun authenticated_logout() = runTest {
        // Setup
        val accountDataFlow = flow { emit(nonEmptyAccountData) }
        every { accountDataRepository.accountData } returns accountDataFlow

        every { inputValidator.validateEmailAddress(any()) } returns true

        coEvery { accountEventBus.onLogout() } returns Unit

        coEvery { authApiClient.logout() } returns Unit

        viewModel = buildViewModel()

        // Initial state tests
        assertEquals(
            AuthenticationState(
                accountData = nonEmptyAccountData,
            ),
            (viewModel.viewState.first() as Ready).authenticationState,
        )

        assertEquals(LoginInputData("email-address"), viewModel.loginInputData)

        // Logout

        // TODO How to test state during authentication?
        viewModel.logout()

        coVerify(exactly = 1) { accountEventBus.onLogout() }

        assertEquals(LoginInputData(), viewModel.loginInputData)

        assertTrue(viewModel.errorMessage.value.isEmpty())
        assertFalse(viewModel.isInvalidEmail.value)
        assertFalse(viewModel.isInvalidPassword.value)
    }

    // TODO Other paths in logout()

    // TODO Save credentials prompts
}
