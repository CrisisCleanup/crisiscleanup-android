package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.feature.caseeditor.model.PropertyInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EditCasePropertyViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    inputValidator: InputValidator,
    appHeaderUiState: AppHeaderUiState,
    private val resourceProvider: AndroidResourceProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val propertyInputData: PropertyInputData

    val navigateBack = mutableStateOf(false)

    private val autoContactOptionValues = listOf(
        AutoContactFrequency.Often,
        AutoContactFrequency.NotOften,
        AutoContactFrequency.Never,
    )

    val autoContactOptions = translator.translationCount.map {
        // TODO Selected value
        autoContactOptionValues.map {
            Pair(it, translator.translate(it.literal) ?: it.literal)
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed(),
    )

    init {
        appHeaderUiState.setTitle(resourceProvider.getString(R.string.property_information))

        propertyInputData = PropertyInputData(
            inputValidator,
            worksiteProvider.editableWorksite,
        )

        val formFields = worksiteProvider.formFields
        logger.logDebug("Property view model", formFields.joinToString(", ") { it.fieldKey })
    }

    fun onNavigateBack(): Boolean {
        return true
    }

    fun onNavigateCancel(): Boolean {
        return true
    }
}