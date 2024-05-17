package com.crisiscleanup.feature.mediamanage

import android.content.ContentResolver
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.crisiscleanup.core.appnav.WorksiteImagesArgs
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.LocalImageRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksiteImageRepository
import com.crisiscleanup.core.designsystem.component.ViewImageViewState
import com.crisiscleanup.core.model.data.CaseImage
import com.crisiscleanup.core.model.data.EmptyWorksite
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class WorksiteImagesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    imageLoader: ImageLoader,
    worksiteImageRepository: WorksiteImageRepository,
    private val localImageRepository: LocalImageRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val contentResolver: ContentResolver,
    private val translator: KeyResourceTranslator,
    private val accountDataRepository: AccountDataRepository,
    private val syncPusher: SyncPusher,
    networkMonitor: NetworkMonitor,
    @Logger(CrisisCleanupLoggers.Media) private val logger: AppLogger,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val worksiteImagesArgs = WorksiteImagesArgs(savedStateHandle)
    private val worksiteId = worksiteImagesArgs.worksiteId

    val screenTitle: String
        get() = translate(worksiteImagesArgs.title)

    private val isImageIndexSetGuard = AtomicBoolean(false)

    private val imageIndex = MutableStateFlow(-1)

    // Decouple selected index to preserve initial matching index value
    var selectedImageIndex by mutableIntStateOf(-1)

    private val isOffline = networkMonitor.isNotOnline
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val worksiteImages = if (worksiteId == EmptyWorksite.id) {
        worksiteImageRepository.streamNewWorksiteImages()
    } else {
        worksiteImageRepository.streamWorksiteImages(worksiteId)
    }

    private val imagesData = combine(
        imageIndex,
        worksiteImages,
        ::Pair,
    )
        .filter { (_, images) -> images.isNotEmpty() }
        .map { (index, images) ->
            val carouselImageIndex = index.coerceIn(0, images.size)
            val image = if (carouselImageIndex < images.size) {
                images[carouselImageIndex]
            } else {
                caseImageNone
            }
            CaseImagePagerData(
                images,
                carouselImageIndex,
                image,
            )
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = CaseImagePagerData(),
            started = SharingStarted.WhileSubscribed(),
        )

    val imageIds = imagesData.mapLatest { it.images.map(CaseImage::imageUri) }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val caseImages = imagesData.mapLatest { it.images }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val selectedImageData = imagesData.mapLatest {
        val index = it.index.coerceIn(0, it.imageCount)
        if (index >= 0 && index < it.images.size) {
            it.images[index]
        } else {
            caseImageNone
        }
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = caseImageNone,
            started = SharingStarted.WhileSubscribed(),
        )

    private val streamNetworkImage = imagesData
        .filter { it.imageData.isNetworkImage }
        .flatMapLatest {
            val url = it.imageData.imageUri
            if (url.isBlank()) {
                return@flatMapLatest flowOf<ViewImageViewState>(ViewImageViewState.None)
            }

            imageLoader.queueNetworkImage(
                context,
                url,
                accountDataRepository,
                isOffline,
                this::translate,
            )
        }

    private val imageState = imagesData
        .flatMapLatest { selectedImage ->
            val imageData = selectedImage.imageData
            if (imageData.isNetworkImage) {
                return@flatMapLatest streamNetworkImage
            } else {
                contentResolver.tryDecodeContentImage(imageData.imageUri, logger)?.let {
                    return@flatMapLatest flowOf(it)
                }

                val message = "worksiteImages.unable_to_load"
                // TODO Delete entry in this case? Think it through
                flowOf(ViewImageViewState.Error(message))
            }
        }

    val viewState = imageState
        .stateIn(
            scope = viewModelScope,
            initialValue = ViewImageViewState.Loading,
            started = SharingStarted.WhileSubscribed(),
        )

    private val rotatingImages = mutableSetOf<String>()
    private val rotatingImagesFlow = MutableStateFlow<Set<String>>(emptySet())

    val enableRotate = combine(
        selectedImageData,
        rotatingImagesFlow,
        ::Pair,
    )
        .map { (image, rotating) ->
            !rotating.contains(image.imageUri)
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val deletingImages = mutableSetOf<String>()
    private val deletingImagesFlow = MutableStateFlow<Set<String>>(emptySet())
    var isDeletedImages by mutableStateOf(false)
        private set
    val enableDelete = combine(
        selectedImageData,
        deletingImagesFlow,
        ::Pair,
    ).map { (image, deleting) ->
        !deleting.contains(image.imageUri)
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        worksiteImages.onEach {
            if (it.isNotEmpty() && !isImageIndexSetGuard.getAndSet(true)) {
                val matchImageId = worksiteImagesArgs.imageId
                val matchImageUri = worksiteImagesArgs.imageUri
                val encodedMatchImageUri = worksiteImagesArgs.encodedUri
                it.forEachIndexed { index, caseImage ->
                    val imageUri = caseImage.imageUri
                    if (caseImage.id > 0 && caseImage.id == matchImageId ||
                        imageUri.isNotBlank() && (imageUri == matchImageUri || imageUri == encodedMatchImageUri)
                    ) {
                        imageIndex.value = index
                        selectedImageIndex = index
                        return@forEachIndexed
                    }
                }
            }
        }
            .launchIn(viewModelScope)
    }

    private fun translate(key: String) = translator(key)

    fun onChangeImageIndex(index: Int) {
        if (index == imageIndex.value) {
            return
        }

        val imageCount = imagesData.value.imageCount
        imageIndex.value = index.coerceIn(0, imageCount)
    }

    fun onOpenImage(index: Int) {
        onChangeImageIndex(index)
        selectedImageIndex = imageIndex.value
    }

    private fun getMatchingImage(imageId: String): CaseImage? = imagesData.value.images
        .firstOrNull { it.imageUri == imageId }

    fun rotateImage(imageId: String, rotateClockwise: Boolean) {
        getMatchingImage(imageId)?.let { matchingImage ->
            synchronized(rotatingImages) {
                if (rotatingImages.contains(imageId)) {
                    return
                }
                rotatingImages.add(imageId)
                rotatingImagesFlow.value = rotatingImages.toSet()
            }

            viewModelScope.launch(ioDispatcher) {
                try {
                    val deltaRotation = if (rotateClockwise) 90 else -90
                    val rotation = (matchingImage.rotateDegrees + deltaRotation) % 360
                    localImageRepository.setImageRotation(
                        matchingImage.id,
                        matchingImage.isNetworkImage,
                        rotation,
                    )
                } finally {
                    synchronized(rotatingImages) {
                        rotatingImages.remove(imageId)
                        rotatingImagesFlow.value = rotatingImages.toSet()
                    }
                }
            }
        }
    }

    fun deleteImage(imageId: String) {
        getMatchingImage(imageId)?.let { matchingImage ->
            synchronized(deletingImages) {
                if (deletingImages.contains(imageId)) {
                    return
                }
                deletingImages.add(imageId)
                deletingImagesFlow.value = deletingImages.toSet()
            }

            viewModelScope.launch(ioDispatcher) {
                val imageCount = imagesData.value.images.size
                try {
                    if (matchingImage.isNetworkImage) {
                        val worksiteId = worksiteChangeRepository.saveDeletePhoto(matchingImage.id)
                        if (worksiteId > 0) {
                            syncPusher.appPushWorksite(worksiteId)
                        }
                    } else {
                        localImageRepository.deleteLocalImage(matchingImage.id)
                    }
                    if (imageCount == 1) {
                        isDeletedImages = true
                    } else {
                        imageIndex.value = imageIndex.value.coerceIn(0, imageCount - 2)
                        selectedImageIndex = imageIndex.value
                    }
                } catch (e: Exception) {
                    // TODO Show error
                    logger.logException(e)
                } finally {
                    synchronized(deletingImages) {
                        deletingImages.remove(imageId)
                        deletingImagesFlow.value = deletingImages.toSet()
                    }
                }
            }
        }
    }
}

data class CaseImagePagerData(
    val images: List<CaseImage> = emptyList(),
    val index: Int = 0,
    val imageData: CaseImage = caseImageNone,
    val imageCount: Int = images.size,
)

private val caseImageNone = CaseImage(
    0,
    false,
    "",
    "",
    "",
)
