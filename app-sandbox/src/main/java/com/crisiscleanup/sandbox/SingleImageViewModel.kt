package com.crisiscleanup.sandbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.designsystem.component.ViewImageViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SingleImageViewModel @Inject constructor() : ViewModel() {
    val imageRotation = MutableStateFlow(0)

    private val imageState = flowOf("")
        .mapLatest {
            ViewImageViewState.Error("Not implemented")
        }

    val viewState = imageState
        .stateIn(
            scope = viewModelScope,
            initialValue = ViewImageViewState.Loading,
            started = SharingStarted.WhileSubscribed(),
        )

    fun rotateImage(rotateClockwise: Boolean) {
        imageRotation.value = (imageRotation.value + (if (rotateClockwise) 90 else -90)) % 360
    }
}
