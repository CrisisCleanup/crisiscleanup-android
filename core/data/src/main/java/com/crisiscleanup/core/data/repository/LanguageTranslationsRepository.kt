package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.di.ApplicationScope
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
import com.crisiscleanup.core.network.model.NetworkLanguageDescription
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface LanguageTranslationsRepository : KeyTranslator {
    val isLoading: Flow<Boolean>

    val supportedLanguages: Flow<List<Language>>

    val currentLanguage: Flow<Language>

    suspend fun loadLanguages(force: Boolean = false)

    fun setLanguage(key: String = "")

    fun setLanguageFromSystem()
}

@Singleton
class OfflineFirstLanguageTranslationsRepository @Inject constructor(
    private val appPreferences: LocalAppPreferencesRepository,
    private val dataSource: CrisisCleanupNetworkDataSource,
    private val languageDao: LanguageDao,
    private val languageDaoPlus: LanguageDaoPlus,
    private val statusRepository: WorkTypeStatusRepository,
    private val resourceProvider: AndroidResourceProvider,
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

    private val languageTranslations = appPreferences.userPreferences.flatMapLatest {
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

    private val translations = languageTranslations
        .mapLatest { it?.translations ?: emptyMap() }
        .stateIn(
            scope = coroutineScope,
            initialValue = emptyMap(),
            started = SharingStarted.WhileSubscribed(),
        )

    override val translationCount = translations
        .mapLatest { it.size }
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

    private suspend fun pullLanguages() = coroutineScope {
        val languageDescriptions =
            dataSource.getLanguages().map(NetworkLanguageDescription::asEntity)
        languageDaoPlus.saveLanguages(languageDescriptions)
    }

    private suspend fun pullTranslations(key: String) = coroutineScope {
        val syncAt = Clock.System.now()
        dataSource.getLanguageTranslations(key)?.let {
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

            setLanguageFromSystem()
        } catch (e: Exception) {
            logger.logException(e)
        } finally {
            isLoadingLanguages.value = false
        }
    }

    override fun setLanguageFromSystem() {
        val systemLocale = Locale.getDefault().toLanguageTag()
        setLanguage(systemLocale)
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
            val languages = supportedLanguages.first()
                .map(Language::key)
                .toSet()
            var languageKey = EnglishLanguage.key
            if (languages.contains(key)) {
                languageKey = key
            } else if (key.contains('-')) {
                val designator = key.split('-')[0]
                if (languages.contains(designator)) {
                    languageKey = designator
                }
            }

            try {
                pullUpdatedTranslations(languageKey)

                ensureActive()

                appPreferences.setLanguageKey(languageKey)
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isSettingLanguage.value = false
            }
        }
    }

    override fun translate(phraseKey: String): String? {
        return translations.value[phraseKey] ?: statusRepository.translateStatus(phraseKey)
    }
}
