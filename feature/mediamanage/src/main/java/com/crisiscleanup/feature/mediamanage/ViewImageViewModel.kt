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
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.LocalImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewImageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    imageLoader: ImageLoader,
    private val localImageRepository: LocalImageRepository,
    private val translator: KeyTranslator,
    accountDataRepository: AccountDataRepository,
    networkMonitor: NetworkMonitor,
    @Logger(CrisisCleanupLoggers.Media) logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val caseEditorArgs = ViewImageArgs(savedStateHandle)

    // Better to go off of image ID in case new URL comes in.
    private val imageId = caseEditorArgs.imageId
    private val isNetworkImage = caseEditorArgs.isNetworkImage
    val screenTitle = caseEditorArgs.title

    val isOffline = networkMonitor.isNotOnline
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val uiState = localImageRepository.streamNetworkImageUrl(imageId).flatMapLatest { url ->
        val imageUrl = url.ifBlank { caseEditorArgs.imageUrl }

        if (imageUrl.isBlank()) {
            // TODO String res
            return@flatMapLatest flowOf(ViewImageUiState.Error("Image not selected"))
        }

        callbackFlow<ViewImageUiState> {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .target(
                    onStart = {
                        channel.trySend(ViewImageUiState.Loading)
                    },
                    onSuccess = { result ->
                        val bitmap = (result as BitmapDrawable).bitmap.asImageBitmap()
                        channel.trySend(ViewImageUiState.Image(bitmap))
                    },
                    onError = {
                        val isTokenInvalid = accountDataRepository.accessTokenCached.isBlank()

                        logger.logDebug("Failed to load image $isOffline $isTokenInvalid $imageId $imageUrl")

                        // TODO Test all three states show correctly
                        // TODO String res
                        val message =
                            if (isOffline.value) "Connect to the internet to download this photo."
                            else if (isTokenInvalid) "Login again and refresh the image."
                            else "Unable to load photo. Try refreshing the image."
                        channel.trySend(ViewImageUiState.Error(message))
                    },
                )
                .build()
            val disposable = imageLoader.enqueue(request)
            awaitClose { disposable.job }
        }
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = ViewImageUiState.Loading,
            started = SharingStarted.WhileSubscribed(),
        )

    val isImageDeletable = uiState.map {
        it is ViewImageUiState.Image && imageId > 0
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    fun translate(key: String) = translator.translate(key) ?: key

    fun deleteImage() {
        if (isNetworkImage) {
            viewModelScope.launch(ioDispatcher) {
                // localImageRepository.deleteNetworkImage(imageId)
            }
        }
    }

    fun rotateImage(rotateClockwise: Boolean) {
        if (isNetworkImage) {
            viewModelScope.launch(ioDispatcher) {
                // localImageRepository.setNetworkImageRotation(imageId, rotation)
            }
        }
    }
}

sealed interface ViewImageUiState {
    object Loading : ViewImageUiState
    data class Image(
        val image: ImageBitmap,
    ) : ViewImageUiState

    data class Error(
        val message: String,
    ) : ViewImageUiState
}