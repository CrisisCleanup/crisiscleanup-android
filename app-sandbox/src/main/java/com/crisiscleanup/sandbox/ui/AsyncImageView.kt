package com.crisiscleanup.sandbox.ui

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.crisiscleanup.core.common.R
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

@Composable
fun AsyncImageView() {
    val imageUrl = ""
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .setHeader("User-Agent", "Mozilla/5.0")
            .build(),
        modifier = Modifier
            .sizeIn(minWidth = 96.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp)),
        placeholder = painterResource(R.drawable.cc_grayscale_pin),
        error = rememberVectorPainter(CrisisCleanupIcons.SyncProblem),
        fallback = rememberVectorPainter(CrisisCleanupIcons.Image),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        onLoading = { loading ->
            Log.i("async-image", "Loading image $loading")
        },
        onSuccess = { success ->
            Log.i("async-image", "Successfully loaded ${success.result}")
        },
        onError = { e ->
            Log.e(
                "async-image",
                "Error loading image ${e.result} for $imageUrl",
                e.result.throwable,
            )
        },
    )
}