package com.crisiscleanup.core.database.di

import android.content.Context
import androidx.room.Room
import com.crisiscleanup.core.common.DatabaseVersionProvider
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.dao.LocalImageDaoPlus
import com.crisiscleanup.core.model.data.PhotoChangeDataProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesCrisisCleanupDatabase(
        @ApplicationContext context: Context,
    ): CrisisCleanupDatabase = Room.databaseBuilder(
        context,
        CrisisCleanupDatabase::class.java,
        "crisis-cleanup-database"
    ).build()

    @Provides
    fun providesPhotoChangeDataProvider(
        db: CrisisCleanupDatabase,
    ): PhotoChangeDataProvider = LocalImageDaoPlus(db)
}

@Module
@InstallIn(SingletonComponent::class)
interface DatabaseInterfaceModule {
    @Binds
    @Singleton
    fun bindsDatabaseVersionProvider(
        versionProvider: CrisisCleanupDatabase,
    ): DatabaseVersionProvider
}
