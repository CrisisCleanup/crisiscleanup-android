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
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksiteImageRepository
import com.crisiscleanup.core.designsystem.component.ViewImageViewState
import com.crisiscleanup.core.model.data.CaseImage
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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class WorksiteImagesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    imageLoader: ImageLoader,
    private val worksiteImageRepository: WorksiteImageRepository,
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

    var selectedImageIndex by mutableIntStateOf(0)
        private set

    private val isOffline = networkMonitor.isNotOnline
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val worksiteImages = combine(
        worksiteImageRepository.streamNewWorksiteImages(),
        worksiteImageRepository.streamWorksiteImages(worksiteId),
        ::Pair,
    )
        .mapLatest { (newImages, existingImages) ->
            newImages.ifEmpty { existingImages }
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

    var isDeletedImages by mutableStateOf(false)
        private set

    init {
        worksiteImages.onEach {
            logger.logDebug("${it.size} ${isImageIndexSetGuard.get()}")
            if (it.isNotEmpty() && !isImageIndexSetGuard.getAndSet(true)) {
                val matchImageId = worksiteImagesArgs.imageId
                val matchImageUri = worksiteImagesArgs.imageUri
                it.forEachIndexed { index, caseImage ->
                    if (caseImage.id > 0 && caseImage.id == matchImageId ||
                        caseImage.imageUri.isNotBlank() && caseImage.imageUri == matchImageUri
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

    fun rotateImage(imageId: String, rotateClockwise: Boolean) {
        // TODO Rotate individual
    }

    fun deleteImage(imageId: String) {
        // TODO Find and delete
        //      Update isDeletedImages
    }
}

data class CaseImagePagerData(
    val images: List<CaseImage> = emptyList(),
    val index: Int = 0,
    val imageData: CaseImage = caseImageNone,
    val imageCount: Int = images.size,
)

private val CaseImage.caseImageId: CaseImageId
    get() = CaseImageId(id, imageUri, isNetworkImage)

data class CaseImageId(
    val id: Long = 0,
    val uri: String = "",
    val isNetworkImage: Boolean = false,
)

private val caseImageNone = CaseImage(
    0,
    false,
    "",
    "",
    "",
)
