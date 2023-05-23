package com.crisiscleanup.feature.mediamanage

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
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
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.LocalImageRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@HiltViewModel
class ViewImageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    imageLoader: ImageLoader,
    private val localImageRepository: LocalImageRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val contentResolver: ContentResolver,
    private val translator: KeyTranslator,
    accountDataRepository: AccountDataRepository,
    private val syncPusher: SyncPusher,
    networkMonitor: NetworkMonitor,
    @Logger(CrisisCleanupLoggers.Media) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val externalScope: CoroutineScope,
) : ViewModel() {
    private val caseEditorArgs = ViewImageArgs(savedStateHandle)
    private val isNetworkImage = caseEditorArgs.isNetworkImage
    private val imageId = caseEditorArgs.imageId
    val screenTitle = caseEditorArgs.title

    val isDeleted = MutableStateFlow(false)

    private val isOffline = networkMonitor.isNotOnline
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val imageRotation = MutableStateFlow(0)

    private val streamNetworkImageState = localImageRepository.streamNetworkImageUrl(imageId)
        .flatMapLatest { url ->
            val imageUrl = url.ifBlank { caseEditorArgs.imageUri }

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
                            val isTokenInvalid =
                                accountDataRepository.accessTokenCached.isBlank()

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

    private val streamLocalImageState = localImageRepository.streamLocalImageUri(imageId)
        .mapLatest { uriString ->
            val uri = Uri.parse(uriString)
            if (uri != null) {
                try {
                    contentResolver.openFileDescriptor(uri, "r").use {
                        it?.let { parcel ->
                            val fileDescriptor = parcel.fileDescriptor
                            val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                            return@mapLatest ViewImageUiState.Image(bitmap.asImageBitmap())
                        }
                    }
                } catch (e: Exception) {
                    logger.logException(e)
                }
            }

            // TODO String res
            val message = "There was an issue loading this image for viewing."
            // TODO Delete entry in this case? Think it through
            ViewImageUiState.Error(message)
        }

    private val imageState = if (isNetworkImage) streamNetworkImageState
    else streamLocalImageState

    val uiState = imageState
        .stateIn(
            scope = viewModelScope,
            initialValue = ViewImageUiState.Loading,
            started = SharingStarted.WhileSubscribed(),
        )

    private val isSyncing = localImageRepository.syncingWorksiteImage.mapLatest {
        it == imageId
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val isImageDeletable = combine(
        uiState,
        isSyncing,
        ::Pair,
    ).map { (state, isSyncingImage) ->
        state is ViewImageUiState.Image &&
                imageId > 0 &&
                !isSyncingImage
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private var savedImageRotation = AtomicReference(999)

    init {
        viewModelScope.launch(ioDispatcher) {
            val rotation = localImageRepository.getImageRotation(imageId, isNetworkImage)
            savedImageRotation = AtomicReference(rotation)
            withContext(Dispatchers.Main) {
                if (rotation != imageRotation.value) {
                    imageRotation.value = rotation
                }
            }
        }

        imageRotation
            .debounce(250)
            .distinctUntilChanged()
            .onEach { rotation ->
                if (savedImageRotation.get() < 999) {
                    localImageRepository.setImageRotation(imageId, isNetworkImage, rotation)
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)
    }

    fun translate(key: String) = translator.translate(key) ?: key

    fun deleteImage() {
        viewModelScope.launch(ioDispatcher) {
            try {
                if (isNetworkImage) {
                    val worksiteId = worksiteChangeRepository.saveDeletePhoto(imageId)
                    if (worksiteId > 0) {
                        externalScope.launch {
                            syncPusher.appPushWorksite(worksiteId)
                        }
                    }
                } else {
                    localImageRepository.deleteLocalImage(imageId)
                }
                isDeleted.value = true
            } catch (e: Exception) {
                // TODO Show error
                logger.logException(e)
            }
        }
    }

    fun rotateImage(rotateClockwise: Boolean) {
        imageRotation.value = (imageRotation.value + (if (rotateClockwise) 90 else -90)) % 360
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