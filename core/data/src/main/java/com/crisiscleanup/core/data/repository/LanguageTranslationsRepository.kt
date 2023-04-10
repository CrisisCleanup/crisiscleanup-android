package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.LanguageDao
import com.crisiscleanup.core.database.dao.LanguageDaoPlus
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.EnglishLanguage
import com.crisiscleanup.core.model.data.Language
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError.Companion.tryThrowException
import com.crisiscleanup.core.network.model.NetworkLanguageDescription
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface LanguageTranslationsRepository : KeyTranslator {
    val isLoading: Flow<Boolean>

    val supportedLanguages: Flow<List<Language>>

    val currentLanguage: Flow<Language>

    suspend fun loadLanguages(force: Boolean = false)

    fun setLanguage(key: String = "")
}

@Singleton
class OfflineFirstLanguageTranslationsRepository @Inject constructor(
    private val appPreferences: LocalAppPreferencesRepository,
    private val dataSource: CrisisCleanupNetworkDataSource,
    private val languageDao: LanguageDao,
    private val languageDaoPlus: LanguageDaoPlus,
    private val authEventManager: AuthEventManager,
    @Logger(CrisisCleanupLoggers.Language) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) : LanguageTranslationsRepository {
    private var isLoadingLanguages = MutableStateFlow(false)
    private var isSettingLanguage = MutableStateFlow(false)

    override val isLoading = combine(
        isLoadingLanguages,
        isSettingLanguage,
    ) { b, b1 -> b || b1 }

    override val supportedLanguages = languageDao.streamLanguages().map {
        it.map { translation -> Language(translation.key, translation.name) }
    }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = listOf(EnglishLanguage),
            started = SharingStarted.WhileSubscribed(5_000),
        )

    private var translationCache = emptyMap<String, String>()

    private val languageTranslations = appPreferences.userData.flatMapLatest {
        val key = it.languageKey.ifEmpty { EnglishLanguage.key }
        languageDao.streamLanguageTranslations(key)
            .mapLatest { translation -> translation?.asExternalModel() }
    }
        .flowOn(ioDispatcher)
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            replay = 1,
        )

    override val translationCount = languageTranslations
        .map { it?.translations?.size ?: 0 }
        .stateIn(
            scope = coroutineScope,
            initialValue = 0,
            started = SharingStarted.WhileSubscribed(),
        )

    override val currentLanguage = languageTranslations.map {
        it?.language ?: EnglishLanguage
    }.stateIn(
        scope = coroutineScope,
        initialValue = EnglishLanguage,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    private var setLanguageJob: Job? = null

    init {
        languageTranslations.onEach {
            translationCache = it?.translations ?: emptyMap()
        }
            .launchIn(coroutineScope)
    }

    private suspend fun pullLanguages() = coroutineScope {
        val languagesResult = dataSource.getLanguages()
        tryThrowException(authEventManager, languagesResult.errors)

        languageDaoPlus.saveLanguages(
            languagesResult.results.map(NetworkLanguageDescription::asEntity)
        )
    }

    private suspend fun pullTranslations(key: String) = coroutineScope {
        val syncAt = Clock.System.now()

        val networkTranslations = dataSource.getLanguageTranslations(key)
        tryThrowException(authEventManager, networkTranslations.errors)

        networkTranslations.translation?.let {
            languageDao.upsertLanguageTranslation(it.asEntity(syncAt))
        }
    }

    override suspend fun loadLanguages(force: Boolean) {
        // TODO Track language sync attempts and skip if not forced and last attempt is recent

        isLoadingLanguages.value = true
        try {
            val languageCount = languageDao.getLanguageCount()
            if (force || languageCount == 0) {
                pullLanguages()
            }

            if (languageCount == 0) {
                pullTranslations(EnglishLanguage.key)
            } else {
                pullUpdatedTranslations()
            }
        } catch (e: Exception) {
            logger.logException(e)
        } finally {
            isLoadingLanguages.value = false
        }
    }

    private suspend fun pullUpdatedTranslations() =
        pullUpdatedTranslations(currentLanguage.value.key)

    private suspend fun pullUpdatedTranslations(key: String) {
        languageDao.streamLanguageTranslations(key).first()?.asExternalModel()?.let {
            val localizationUpdateCount = dataSource.getLocalizationCount(it.syncedAt)
            if ((localizationUpdateCount.count ?: 0) > 0) {
                pullLanguages()
                pullTranslations(key)
            }
        }
    }

    override fun setLanguage(key: String) {
        setLanguageJob?.cancel()
        setLanguageJob = coroutineScope.launch(ioDispatcher) {
            try {
                // TODO Set the language if local translations exist.
                //      Pull does not need to succeed in this case.
                //      Consider possible race condition if ordering changes.

                pullUpdatedTranslations(key)

                ensureActive()

                appPreferences.setLanguageKey(key)
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isSettingLanguage.value = false
            }
        }
    }

    override fun translate(phraseKey: String): String? = translationCache[phraseKey]
}