package com.crisiscleanup.feature.mediamanage

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.crisiscleanup.core.appnav.ViewImageArgs
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewImageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    imageLoader: ImageLoader,
    private val translator: KeyTranslator,
    networkMonitor: NetworkMonitor,
    @Logger(CrisisCleanupLoggers.Media) logger: AppLogger,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val caseEditorArgs = ViewImageArgs(savedStateHandle)

    // Better to go off of image ID in case new URL comes in.
    private val imageId = caseEditorArgs.imageId
    val imageUrl = caseEditorArgs.imageUrl
    private val isNetworkImage = caseEditorArgs.isNetworkImage
    val screenTitle = caseEditorArgs.title

    private val isOnline = networkMonitor.isOnline.stateIn(
        scope = viewModelScope,
        initialValue = false,
        started = SharingStarted.WhileSubscribed(),
    )

    val uiState: MutableStateFlow<ViewImageUiState> = MutableStateFlow(ViewImageUiState.Loading)

    val isImageDeletable = uiState.map {
        it is ViewImageUiState.Image && imageId > 0
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        if (imageUrl.isBlank()) {
            // TODO String res
            uiState.value = ViewImageUiState.Error("No image is selected")
        } else {
            viewModelScope.launch(ioDispatcher) {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .target(
                        onSuccess = { result ->
                            val bitmap = (result as BitmapDrawable).bitmap.asImageBitmap()
                            uiState.value = ViewImageUiState.Image(bitmap)
                        },
                        onError = {
                            // TODO Better visual or change background as may not be clear/visible
                            // TODO String res
                            val message = if (isOnline.value) "Unable to load photo"
                            else "Connect to the internet to download this photo"
                            uiState.value = ViewImageUiState.Error(
                                message,
                                isOnline.value,
                            )

                            logger.logDebug("Failed to load image ${isOnline.value} $imageId $imageUrl")
                        },
                    )
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    fun translate(key: String) = translator.translate(key) ?: key

    fun deleteImage() {
        // TODO
    }
}

sealed interface ViewImageUiState {
    object Loading : ViewImageUiState
    data class Image(
        val image: ImageBitmap,
    ) : ViewImageUiState

    data class Error(
        val message: String,
        val hasInternet: Boolean = false,
    ) : ViewImageUiState
}