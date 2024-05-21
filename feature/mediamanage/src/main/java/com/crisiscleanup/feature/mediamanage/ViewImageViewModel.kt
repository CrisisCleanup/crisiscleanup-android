package com.crisiscleanup.feature.mediamanage

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.crisiscleanup.core.appnav.ViewImageArgs
import com.crisiscleanup.core.common.KeyResourceTranslator
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
import com.crisiscleanup.core.designsystem.component.ViewImageViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

@OptIn(FlowPreview::class)
@HiltViewModel
class ViewImageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    imageLoader: ImageLoader,
    private val localImageRepository: LocalImageRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val contentResolver: ContentResolver,
    private val translator: KeyResourceTranslator,
    accountDataRepository: AccountDataRepository,
    private val syncPusher: SyncPusher,
    networkMonitor: NetworkMonitor,
    @Logger(CrisisCleanupLoggers.Media) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val viewImageArgs = ViewImageArgs(savedStateHandle)
    private val isNetworkImage = viewImageArgs.isNetworkImage
    private val imageId = viewImageArgs.imageId

    val screenTitle: String
        get() = translate(viewImageArgs.title)

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
            val imageUrl = if (isNotBlankUrl) url!! else viewImageArgs.imageUri

            if (imageUrl.isBlank()) {
                // TODO String res
                return@flatMapLatest flowOf(ViewImageViewState.Error("Image not selected"))
            }

            imageLoader.queueNetworkImage(
                context,
                imageUrl,
                accountDataRepository,
                isOffline,
                this::translate,
            )
        }

    private val streamLocalImageState = localImageRepository.streamLocalImageUri(imageId)
        .filter { it?.isNotEmpty() == true }
        .mapLatest { uriString ->
            contentResolver.tryDecodeContentImage(uriString, logger)?.let {
                return@mapLatest it
            }

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

    private fun translate(key: String) = translator(key)

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

fun ContentResolver.tryDecodeContentImage(
    uriString: String?,
    logger: AppLogger,
): ViewImageViewState.Image? {
    val uri = Uri.parse(uriString)
    if (uri != null) {
        try {
            openFileDescriptor(uri, "r").use {
                it?.let { parcel ->
                    val fileDescriptor = parcel.fileDescriptor
                    val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor).asImageBitmap()
                    bitmap.prepareToDraw()
                    return ViewImageViewState.Image(
                        uriString!!,
                        bitmap,
                    )
                }
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    return null
}

internal fun ImageLoader.queueNetworkImage(
    context: Context,
    imageUrl: String,
    accountDataRepository: AccountDataRepository,
    isOffline: StateFlow<Boolean>,
    translate: (String) -> String,
) = callbackFlow {
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .target(
            onStart = {
                channel.trySend(ViewImageViewState.Loading)
            },
            onSuccess = { result ->
                val bitmap = (result as BitmapDrawable).bitmap.asImageBitmap()
                bitmap.prepareToDraw()
                channel.trySend(ViewImageViewState.Image(imageUrl, bitmap))
            },
            onError = {
                val isTokenInvalid =
                    accountDataRepository.refreshToken.isBlank()

                // TODO Test all three states show correctly
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
    val disposable = enqueue(request)
    awaitClose { disposable.job }
}
