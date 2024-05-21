package com.crisiscleanup.sandbox

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.crisiscleanup.core.designsystem.component.ViewImageViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MultiImageViewModel @Inject constructor(
    @ApplicationContext context: Context,
    imageLoader: ImageLoader,
) : ViewModel() {
    val imageRotation = MutableStateFlow(0)

    private val imageBasePath = "http://10.0.2.2:8080/rectangles"

    // Replace with any hosted image
    private val imageUrls = MutableStateFlow(
        listOf(
            "$imageBasePath/rectangle-1080-2280-128-90CAF91A237E-32-000.png",
            "$imageBasePath/rectangle-1200-1920-128-90CAF91A237E-32-000.png",
            "$imageBasePath/rectangle-1200-2800-128-90CAF91A237E-32-000.png",
            "$imageBasePath/rectangle-1920-1200-128-90CAF91A237E-32-000.png",
            "$imageBasePath/rectangle-1920-900-128-90CAF91A237E-32-000.png",
            "$imageBasePath/rectangle-2800-1200-128-90CAF91A237E-32-000.png",
            "$imageBasePath/rectangle-900-2800-128-90CAF91A237E-32-000.png",
        ),
    )

    private val imageIndex = MutableStateFlow(0)

    val imagesData = combine(
        imageIndex,
        imageUrls,
        ::Pair,
    )
        .map { (index, imageUrls) ->
            val carouselImageIndex = index.coerceIn(0, imageUrls.size)
            val imageUrl = if (carouselImageIndex < imageUrls.size) {
                imageUrls[carouselImageIndex]
            } else {
                ""
            }
            UrlImagePagerData(
                imageUrls,
                carouselImageIndex,
                imageUrl,
            )
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = UrlImagePagerData(),
            started = SharingStarted.WhileSubscribed(),
        )

    private val imageState = imagesData
        .map { it.imageUrl }
        .flatMapLatest { url ->
            if (url.isBlank()) {
                return@flatMapLatest flowOf<ViewImageViewState>(ViewImageViewState.None)
            }

            callbackFlow {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .target(
                        onStart = {
                            channel.trySend(ViewImageViewState.Loading)
                        },
                        onSuccess = { result ->
                            val bitmap = (result as BitmapDrawable).bitmap.asImageBitmap()
                            ensureActive()
                            bitmap.prepareToDraw()
                            ensureActive()
                            channel.trySend(ViewImageViewState.Image(url, bitmap))
                        },
                        onError = {
                            ensureActive()
                            val message = "Issue downloading image $url"
                            channel.trySend(ViewImageViewState.Error(message))
                        },
                    )
                    .build()
                val disposable = imageLoader.enqueue(request)
                awaitClose { disposable.job }
            }
        }

    val viewState = imageState
        .stateIn(
            scope = viewModelScope,
            initialValue = ViewImageViewState.Loading,
            started = SharingStarted.WhileSubscribed(),
        )

    fun onChangeImageIndex(index: Int) {
        if (index == imageIndex.value) {
            return
        }

        val imageCount = imagesData.value.imageCount
        imageIndex.value = index.coerceIn(0, imageCount)
    }

    fun rotateImage(imageId: String, rotateClockwise: Boolean) {
        imageRotation.value = (imageRotation.value + (if (rotateClockwise) 90 else -90)) % 360
    }
}

data class UrlImagePagerData(
    val imageUrls: List<String> = emptyList(),
    val index: Int = 0,
    val imageUrl: String = "",
    val imageCount: Int = imageUrls.size,
)
