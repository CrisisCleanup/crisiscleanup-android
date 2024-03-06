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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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
) : ViewModel() {
    private val caseEditorArgs = ViewImageArgs(savedStateHandle)
    private val isNetworkImage = caseEditorArgs.isNetworkImage
    private val imageId = caseEditorArgs.imageId
    val screenTitle = caseEditorArgs.title

    private val isDeleting = MutableStateFlow(false)
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
            val isNotBlankUrl = url?.isNotBlank() == true
            val imageUrl = if (isNotBlankUrl) url!! else caseEditorArgs.imageUri

            if (imageUrl.isBlank()) {
                // TODO String res
                return@flatMapLatest flowOf(ViewImageViewState.Error("Image not selected"))
            }

            callbackFlow<ViewImageViewState> {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .target(
                        onStart = {
                            channel.trySend(ViewImageViewState.Loading)
                        },
                        onSuccess = { result ->
                            val bitmap = (result as BitmapDrawable).bitmap.asImageBitmap()
                            channel.trySend(ViewImageViewState.Image(bitmap))
                        },
                        onError = {
                            val isTokenInvalid =
                                accountDataRepository.refreshToken.isBlank()

                            logger.logDebug("Failed to load image $isOffline $isTokenInvalid $imageId $imageUrl")

                            // TODO Test all three states show correctly
                            // TODO String res
                            val messageKey =
                                if (isOffline.value) {
                                    "worksiteImages.connect_to_download_photo"
                                } else if (isTokenInvalid) {
                                    "worksiteImages.login_and_refresh_image"
                                } else {
                                    "worksiteImages.try_refreshing_open_image"
                                }
                            val message = translate(messageKey)
                            channel.trySend(ViewImageViewState.Error(message))
                        },
                    )
                    .build()
                val disposable = imageLoader.enqueue(request)
                awaitClose { disposable.job }
            }
        }

    private val streamLocalImageState = localImageRepository.streamLocalImageUri(imageId)
        .filter { it?.isNotEmpty() == true }
        .mapLatest { uriString ->
            val uri = Uri.parse(uriString)
            if (uri != null) {
                try {
                    contentResolver.openFileDescriptor(uri, "r").use {
                        it?.let { parcel ->
                            val fileDescriptor = parcel.fileDescriptor
                            val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                            return@mapLatest ViewImageViewState.Image(bitmap.asImageBitmap())
                        }
                    }
                } catch (e: Exception) {
                    logger.logException(e)
                }
            }

            // TODO String res
            val message = "worksiteImages.unable_to_load"
            // TODO Delete entry in this case? Think it through
            ViewImageViewState.Error(message)
        }

    private val imageState = if (isNetworkImage) {
        streamNetworkImageState
    } else {
        streamLocalImageState
    }

    val viewState = imageState
        .stateIn(
            scope = viewModelScope,
            initialValue = ViewImageViewState.Loading,
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
        viewState,
        isSyncing,
        isDeleting,
        ::Triple,
    ).map { (state, syncing, deleting) ->
        state is ViewImageViewState.Image &&
            imageId > 0 &&
            !(syncing || deleting)
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val savedImageRotation = AtomicReference(999)

    init {
        viewModelScope.launch(ioDispatcher) {
            val rotation = localImageRepository.getImageRotation(imageId, isNetworkImage)
            withContext(Dispatchers.Main) {
                if (rotation != imageRotation.value) {
                    imageRotation.value = rotation
                }
                savedImageRotation.set(rotation)
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
        isDeleting.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                if (isNetworkImage) {
                    val worksiteId = worksiteChangeRepository.saveDeletePhoto(imageId)
                    if (worksiteId > 0) {
                        syncPusher.appPushWorksite(worksiteId)
                    }
                } else {
                    localImageRepository.deleteLocalImage(imageId)
                }
                isDeleted.value = true
            } catch (e: Exception) {
                // TODO Show error
                logger.logException(e)
            } finally {
                isDeleting.value = false
            }
        }
    }

    fun rotateImage(rotateClockwise: Boolean) {
        imageRotation.value = (imageRotation.value + (if (rotateClockwise) 90 else -90)) % 360
    }
}

sealed interface ViewImageViewState {
    data object Loading : ViewImageViewState
    data class Image(
        val image: ImageBitmap,
    ) : ViewImageViewState

    data class Error(
        val message: String,
    ) : ViewImageViewState
}
