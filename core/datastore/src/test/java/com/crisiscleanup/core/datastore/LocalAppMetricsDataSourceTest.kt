package com.crisiscleanup.core.datastore

import app.cash.turbine.test
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.datastore.test.testAppMetricsDataStore
import com.crisiscleanup.core.model.data.AppOpenInstant
import com.crisiscleanup.core.testing.util.nowTruncateMillis
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class LocalAppMetricsDataSourceTest {
    @MockK
    lateinit var appVersionProvider: AppVersionProvider

    private lateinit var subject: LocalAppMetricsDataSource

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        subject = LocalAppMetricsDataSource(
            tmpFolder.testAppMetricsDataStore(),
            appVersionProvider,
        )
    }

    private fun mockVersionCodes() {
        coEvery {
            appVersionProvider.versionCode
        } returnsMany listOf(23, 34, 45, 56, 67, 78)
    }

    @Test
    fun appOpen() = runTest {
        mockVersionCodes()

        subject.metrics.test {
            val initial = awaitItem()

            assertEquals(
                AppOpenInstant(
                    0,
                    Instant.fromEpochSeconds(0),
                ),
                initial.appOpen,
            )
            assertEquals(0, initial.appInstallVersion)

            val firstOpenTimestamp = nowTruncateMillis.minus(10.seconds)
            subject.setAppOpen(firstOpenTimestamp)

            val firstOpen = awaitItem()
            // Skip first version provided due to metrics sequence accessing version
            assertEquals(
                AppOpenInstant(
                    34,
                    firstOpenTimestamp,
                ),
                firstOpen.appOpen,
            )
            assertEquals(34, firstOpen.appInstallVersion)

            val secondOpenTimestamp = nowTruncateMillis.minus(3.seconds)
            subject.setAppOpen(secondOpenTimestamp)

            val secondOpen = awaitItem()
            // Skip another version provided due to metrics sequence accessing version
            assertEquals(
                AppOpenInstant(
                    56,
                    secondOpenTimestamp,
                ),
                secondOpen.appOpen,
            )
            assertEquals(34, secondOpen.appInstallVersion)
        }
    }
}
