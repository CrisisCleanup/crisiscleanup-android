package com.crisiscleanup.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

object TestUtil {
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