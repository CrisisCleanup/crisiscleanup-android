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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SingleImageViewModel @Inject constructor(
    @ApplicationContext context: Context,
    imageLoader: ImageLoader,
) : ViewModel() {
    val imageRotation = MutableStateFlow(0)

    // Replace with any hosted image
    private val imageUrl =
        "http://10.0.2.26:8080/rectangles/rectangle-1200-2800-128-90CAF91A237E-32-000.png"

    private val imageState =
        flowOf(imageUrl)
            .flatMapLatest { url ->
                callbackFlow {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .target(
                            onStart = {
                                channel.trySend(ViewImageViewState.Loading)
                            },
                            onSuccess = { result ->
                                val bitmap = (result as BitmapDrawable).bitmap.asImageBitmap()
                                bitmap.prepareToDraw()
                                channel.trySend(ViewImageViewState.Image(url, bitmap))
                            },
                            onError = {
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

    fun rotateImage(rotateClockwise: Boolean) {
        imageRotation.value = (imageRotation.value + (if (rotateClockwise) 90 else -90)) % 360
    }
}
