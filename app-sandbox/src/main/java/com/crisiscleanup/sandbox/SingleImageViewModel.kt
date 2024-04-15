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

    // Renew access if expired
    private val imageUrl =
        "https://cdn.crisiscleanup.org/20230830_144035-724b5cb26a714ff2b74b84ecc32b8f4c.jpg?Expires=1713207624&Signature=QkU-uPy0tDuc66HW~GCMoU1-TqpLNRouPxicJNwuZfDFJw5YMEHnKgASS3Bvsj4tsL1vvEN3BQvEGILhUBur4q98cGUqJP2yqLIu43~ncQFfqseGbX1JzOClqD6J58h0Ei6plTWMCCKF0Ajr~7JBH4jMBdI6EROCfY1dJ-OZhDhsHg4HlWRyrJ96vqUAiBugC5USAeJfl47CXMR37eRslXeoi6Ko-siMUPqcPu5LYBz~Bc4MeOEyaHi07Dbn73xU2sxvQDqGIhPEPQLhCBRo3f1d1izHy4FUVZYFULTCzH1Ac-UjHr9Rf8VywD93kgfKN6h4HgeewbDcsMpG5XH3tw__&Key-Pair-Id=K1ZA20TYD9PJA4"

    private val imageState =
        flowOf(imageUrl)
            .flatMapLatest { imageUrl ->
                callbackFlow {
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
                                val message = "Issue downloading image $imageUrl"
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
