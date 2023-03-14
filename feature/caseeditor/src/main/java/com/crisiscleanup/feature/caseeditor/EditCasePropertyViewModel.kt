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
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.feature.caseeditor.model.PropertyInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EditCasePropertyViewModel @Inject constructor(
    private val worksiteProvider: EditableWorksiteProvider,
    inputValidator: InputValidator,
    appHeaderUiState: AppHeaderUiState,
    resourceProvider: AndroidResourceProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : ViewModel() {
    val propertyInputData: PropertyInputData

    val navigateBack = mutableStateOf(false)

    private val contactFrequencyOptionValues = listOf(
        AutoContactFrequency.Often,
        AutoContactFrequency.NotOften,
        AutoContactFrequency.Never,
    )

    val contactFrequencyOptions = translator.translationCount.map {
        contactFrequencyOptionValues.map {
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
            worksiteProvider.editableWorksite.value,
            resourceProvider,
        )
    }

    private fun validateSaveWorksite(): Boolean {
        val updatedWorksite = propertyInputData.updateCase()
        if (updatedWorksite != null) {
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }

    fun onSystemBack(): Boolean {
        return validateSaveWorksite()
    }

    fun onNavigateBack(): Boolean {
        return validateSaveWorksite()
    }

    fun onNavigateCancel(): Boolean {
        return true
    }
}