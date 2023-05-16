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
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.urlDecode
import com.crisiscleanup.core.data.repository.AccountDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewImageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    imageLoader: ImageLoader,
    accountDataRepository: AccountDataRepository,
    networkMonitor: NetworkMonitor,
    @Logger(CrisisCleanupLoggers.Media) logger: AppLogger,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val caseEditorArgs = ViewImageArgs(savedStateHandle)
    private val imageId = caseEditorArgs.imageId
    val imageUrl = caseEditorArgs.imageUrl.urlDecode()
    private val isNetworkImage = caseEditorArgs.isNetworkImage

    private val isOnline = networkMonitor.isOnline.stateIn(
        scope = viewModelScope,
        initialValue = false,
        started = SharingStarted.WhileSubscribed(),
    )

    val uiState: MutableStateFlow<ViewImageUiState> = MutableStateFlow(ViewImageUiState.Loading)

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
                            val width = bitmap.width
                            val height = bitmap.height
                            // TODO Determine min and max scales
                            uiState.value = ViewImageUiState.Image(bitmap)
                        },
                        onError = {
                            // TODO String res
                            val message = if (isOnline.value) "Unable to load photo"
                            else "Connect to the internet to download this photo"
                            val isInvalidToken = false
//                                accountDataRepository.accountData.first().isTokenInvalid
                            uiState.value = ViewImageUiState.Error(
                                message,
                                isOnline.value,
                                isInvalidToken = isInvalidToken,
                            )

                            logger.logDebug("Failed to load image ${isOnline.value} $isInvalidToken $imageId $imageUrl")
                        },
                    )
                    .build()
                imageLoader.enqueue(request)
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
        val hasInternet: Boolean = false,
        val isInvalidToken: Boolean = false,
    ) : ViewImageUiState
}