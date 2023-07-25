package com.crisiscleanup.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.UuidGenerator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteChangeSerializer
import io.mockk.spyk
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object TestUtil {
    // TODO Change spys to mock when https://github.com/mockk/mockk/issues/1035 is fixed

    fun testUuidGenerator(): UuidGenerator = spyk(object : UuidGenerator {
        private val counter = AtomicInteger()
        override fun uuid() = "uuid-${counter.incrementAndGet()}"
    })

    fun testChangeSerializer(): WorksiteChangeSerializer =
        spyk(object : WorksiteChangeSerializer {
            override fun serialize(
                isDataChange: Boolean,
                worksiteStart: Worksite,
                worksiteChange: Worksite,
                flagIdLookup: Map<Long, Long>,
                noteIdLookup: Map<Long, Long>,
                workTypeIdLookup: Map<Long, Long>,
                requestReason: String,
                requestWorkTypes: List<String>,
                releaseReason: String,
                releaseWorkTypes: List<String>,
            ) = Pair(1, "test-worksite-change")
        })

    fun testAppVersionProvider(): AppVersionProvider = spyk(object : AppVersionProvider {
        override val version: Pair<Long, String> = Pair(81, "1.0.81")
        override val versionCode: Long = version.first
        override val versionName: String = version.second
    })

    fun testAppLogger(): AppLogger = spyk(object : AppLogger {
        override fun logDebug(vararg logs: Any) {}

        override fun logException(e: Exception) {}

        override fun logCapture(message: String) {}
    })

    fun testSyncLogger(): SyncLogger = spyk(object : SyncLogger {
        override var type: String = "test"

        override fun log(message: String, details: String, type: String) = this

        override fun clear() = this

        override fun flush() {}
    })

    fun getDatabase(): CrisisCleanupDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(
            context,
            CrisisCleanupDatabase::class.java
        ).build()
    }

    fun getTestDatabase(): TestCrisisCleanupDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(
            context,
            TestCrisisCleanupDatabase::class.java
        ).build()
    }
}

fun Instant.isNearNow(duration: Duration = 1.seconds) = Clock.System.now().minus(this) < duration