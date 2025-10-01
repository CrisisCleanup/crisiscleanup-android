package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.data.ClaimCloseCounts
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.WorkTypeAnalyzer
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.database.model.IncidentClaimThresholdEntity
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.model.data.AppConfigData
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.model.data.emptyAccountData
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IncidentClaimThresholdRepositoryTest {
    @MockK
    private lateinit var incidentDao: IncidentDao

    @MockK
    private lateinit var incidentDaoPlus: IncidentDaoPlus

    @MockK
    private lateinit var accountInfoDataSource: AccountInfoDataSource

    @MockK
    private lateinit var workTypeAnalyzer: WorkTypeAnalyzer

    @MockK
    private lateinit var appConfigRepository: AppConfigRepository

    @MockK
    private lateinit var incidentSelector: IncidentSelector

    @MockK
    private lateinit var logger: AppLogger
    private val slotException = slot<Exception>()

    private lateinit var claimThresholdRepository: IncidentClaimThresholdRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { incidentSelector.incidentId } returns MutableStateFlow(34)
        every {
            accountInfoDataSource.accountData
        } returns flowOf(
            emptyAccountData.copy(
                id = 84,
                org = OrgData(77, "org"),
            ),
        )

        every { logger.logException(capture(slotException)) } just runs

        claimThresholdRepository = CrisisCleanupIncidentClaimThresholdRepository(
            incidentDao,
            incidentDaoPlus,
            accountInfoDataSource,
            workTypeAnalyzer,
            appConfigRepository,
            incidentSelector,
            logger,
        )
    }

    private fun makeClaimThresholdEntity(
        claimCount: Int = 0,
        closeRatio: Float = 0f,
        accountId: Long = 84,
        incidentId: Long = 34,
    ) = IncidentClaimThresholdEntity(
        userId = accountId,
        incidentId = incidentId,
        userClaimCount = claimCount,
        userCloseRatio = closeRatio,
    )

    @Test
    fun nonPositiveClaimCount() = runTest {
        for (i in -1..0) {
            val isUnderClaimThreshold = claimThresholdRepository.isWithinClaimCloseThreshold(1, i)
            assertTrue(isUnderClaimThreshold)
            verify(exactly = 0) {
                incidentDao.getIncidentClaimThreshold(any(), any())
            }
        }
    }

    @Test
    fun skipUnsynced() = runTest {
        claimThresholdRepository.onWorksiteCreated(354)

        every { appConfigRepository.appConfig } returns flowOf(
            AppConfigData(10, 0.5f),
        )

        var dbCallCounter = 0
        val dbResults = listOf(
            makeClaimThresholdEntity(0, 0f),
            makeClaimThresholdEntity(10, 0.1f),
            makeClaimThresholdEntity(11, 0.1f),
            makeClaimThresholdEntity(9, 0.5f),
            makeClaimThresholdEntity(9, 0.5001f),
            makeClaimThresholdEntity(10, 0.5f),
            makeClaimThresholdEntity(11, 0.5001f),
            null,
        )
        every {
            incidentDao.getIncidentClaimThreshold(accountId = 84, incidentId = 34)
        } answers {
            dbResults[dbCallCounter++]
        }

        val expectedUnder = listOf(
            true,
            false,
            false,
            true,
            true,
            true,
            true,
        )
        for (i in expectedUnder.indices) {
            val actual = claimThresholdRepository.isWithinClaimCloseThreshold(354, 1)
            assertEquals(expectedUnder[i], actual, "$i")
        }

        verify(exactly = 0) {
            workTypeAnalyzer.countUnsyncedClaimCloseWork(
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun unsyncedCounts() = runTest {
        every { appConfigRepository.appConfig } returns flowOf(
            AppConfigData(20, 0.5f),
        )

        every {
            incidentDao.getIncidentClaimThreshold(accountId = 84, incidentId = 34)
        } returns makeClaimThresholdEntity(10, 0.5f)

        var analyzerCallCounter = 0
        val analyzerResults = listOf(
            ClaimCloseCounts(9, 4),
            ClaimCloseCounts(10, 4),
            ClaimCloseCounts(11, 5),
            ClaimCloseCounts(9, 5),
            ClaimCloseCounts(9, 6),
            ClaimCloseCounts(10, 5),
            ClaimCloseCounts(11, 6),
        )
        every {
            workTypeAnalyzer.countUnsyncedClaimCloseWork(
                77,
                34,
                emptySet(),
            )
        } answers {
            analyzerResults[analyzerCallCounter++]
        }

        val expectedUnder = listOf(
            true,
            false,
            false,
            true,
            true,
            true,
            true,
        )
        for (i in expectedUnder.indices) {
            val actual = claimThresholdRepository.isWithinClaimCloseThreshold(354, 1)
            assertEquals(expectedUnder[i], actual, "$i")
        }
    }

    @Test
    fun unsyncedNegativeClaimCounts() = runTest {
        every { appConfigRepository.appConfig } returns flowOf(
            AppConfigData(20, 0.5f),
        )

        every {
            incidentDao.getIncidentClaimThreshold(accountId = 84, incidentId = 34)
        } returns makeClaimThresholdEntity(30, 0.3f)

        var analyzerCallCounter = 0
        val analyzerResults = listOf(
            ClaimCloseCounts(-9, 1),
            ClaimCloseCounts(-10, 0),
            ClaimCloseCounts(-10, 1),
            ClaimCloseCounts(-11, 0),
        )
        every {
            workTypeAnalyzer.countUnsyncedClaimCloseWork(
                77,
                34,
                emptySet(),
            )
        } answers {
            analyzerResults[analyzerCallCounter++]
        }

        val expectedUnder = listOf(
            false,
            false,
            true,
            true,
        )
        for (i in expectedUnder.indices) {
            val actual = claimThresholdRepository.isWithinClaimCloseThreshold(354, 1)
            assertEquals(expectedUnder[i], actual, "$i")
        }
    }

    @Test
    fun unsyncedNegativeCloseCounts() = runTest {
        every { appConfigRepository.appConfig } returns flowOf(
            AppConfigData(20, 0.5f),
        )

        every {
            incidentDao.getIncidentClaimThreshold(accountId = 84, incidentId = 34)
        } returns makeClaimThresholdEntity(16, 0.75f)

        var analyzerCallCounter = 0
        val analyzerResults = listOf(
            ClaimCloseCounts(4, -2),
            ClaimCloseCounts(4, -1),
            ClaimCloseCounts(4, -2),
            ClaimCloseCounts(4, -3),
        )
        every {
            workTypeAnalyzer.countUnsyncedClaimCloseWork(
                77,
                34,
                emptySet(),
            )
        } answers {
            analyzerResults[analyzerCallCounter++]
        }

        val expectedUnder = listOf(
            true,
            true,
            true,
            false,
        )
        for (i in expectedUnder.indices) {
            val actual = claimThresholdRepository.isWithinClaimCloseThreshold(354, 1)
            assertEquals(expectedUnder[i], actual, "$i")
        }
    }

    @Test
    fun analyzerException() = runTest {
        every { appConfigRepository.appConfig } returns flowOf(
            AppConfigData(20, 0.5f),
        )

        every {
            incidentDao.getIncidentClaimThreshold(accountId = 84, incidentId = 34)
        } returns makeClaimThresholdEntity(19, 0.49999f)

        every {
            workTypeAnalyzer.countUnsyncedClaimCloseWork(
                77,
                34,
                emptySet(),
            )
        } throws (Exception("test-exception"))

        val actual = claimThresholdRepository.isWithinClaimCloseThreshold(354, 1)
        assertEquals(true, actual)

        assertEquals("test-exception", slotException.captured.message)
    }
}
