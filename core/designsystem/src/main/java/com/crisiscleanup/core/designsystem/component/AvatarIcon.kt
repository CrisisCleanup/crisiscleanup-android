package com.crisiscleanup.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

@Composable
fun AvatarIcon(
    avatarUri: String?,
    contentDescription: String,
    errorIcon: ImageVector = CrisisCleanupIcons.MissingAvatar,
    fallbackIcon: ImageVector = CrisisCleanupIcons.Account,
    placeholderIcon: ImageVector = CrisisCleanupIcons.Account,
) {
    if (avatarUri?.isNotBlank() == true) {
        var model: Any? = avatarUri
        // TODO Make parameter?
        if (avatarUri.contains("api.dicebear.com")) {
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUri)
                .decoderFactory(SvgDecoder.Factory())
                .build()
        }
        val errorPainter = rememberVectorPainter(errorIcon)
        val fallbackIconPainter = rememberVectorPainter(fallbackIcon)
        val placeholderIconPainter = rememberVectorPainter(placeholderIcon)
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            error = errorPainter,
            fallback = fallbackIconPainter,
            placeholder = placeholderIconPainter,
        )
    } else {
        Icon(
            imageVector = fallbackIcon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}