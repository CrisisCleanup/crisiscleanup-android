package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.designsystem.theme.primaryBlueOneTenthColor
import com.crisiscleanup.core.model.data.NetworkImage
import com.crisiscleanup.core.ui.touchDownConsumer
import com.crisiscleanup.feature.caseeditor.R

private val addMediaActionPadding = 4.dp
private val addMediaCornerRadius = 4.dp
private val addMediaStrokeWidth = 2.dp
private val addMediaStrokeDash = 6.dp
private val addMediaStrokeGap = 6.dp

@Composable
private fun AddMediaView(
    modifier: Modifier = Modifier,
) {
    val text = stringResource(R.string.add_media)
    val contentColor = primaryBlueColor
    val backgroundColor = primaryBlueOneTenthColor
    val cornerRadius: Float
    val strokeDash: Float
    val strokeGap: Float
    val strokeWidth: Float
    with(LocalDensity.current) {
        cornerRadius = addMediaCornerRadius.toPx()
        strokeWidth = addMediaStrokeWidth.toPx()
        strokeDash = addMediaStrokeDash.toPx()
        strokeGap = addMediaStrokeGap.toPx()
    }
    val borderStroke = Stroke(
        width = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeDash, strokeGap), 0f)
    )
    Surface(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = contentColor,
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = borderStroke,
                )
            },
        color = backgroundColor,
        shape = RoundedCornerShape(addMediaCornerRadius),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = CrisisCleanupIcons.Add,
                contentDescription = text,
                tint = contentColor,
            )
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = contentColor,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun PhotosSection(
    title: String,
    photoRowModifier: Modifier = Modifier,
    photoRowGridCells: StaggeredGridCells = StaggeredGridCells.Fixed(1),
    photos: List<NetworkImage> = emptyList(),
    onAddPhoto: (String) -> Unit = {},
    onPhotoSelect: (NetworkImage) -> Unit = {},
    isEditable: Boolean = false,
    setEnableParentScroll: (Boolean) -> Unit = {},
    addActionSize: Dp = 128.dp,
) {
    Text(
        title,
        listItemModifier,
        style = MaterialTheme.typography.titleMedium,
    )

    val gridState = rememberLazyStaggeredGridState()
    LaunchedEffect(gridState.isScrollInProgress) {
        if (!gridState.isScrollInProgress) {
            setEnableParentScroll(true)
        }
    }
    LazyHorizontalStaggeredGrid(
        modifier = photoRowModifier.touchDownConsumer { setEnableParentScroll(false) },
        rows = photoRowGridCells,
        state = gridState,
        horizontalItemSpacing = 1.dp,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        items(
            1 + photos.size,
            key = {
                if (it == 0) "add-media"
                else photos[it - 1].imageUrl
            },
            contentType = {
                if (it == 0) "add-media"
                else "photo"
            }
        ) { index ->
            if (index == 0) {
                AddMediaView(
                    Modifier
                        .padding(addMediaActionPadding)
                        .clickable { onAddPhoto(title) }
                        .size(addActionSize),
                )
            } else {
                val photoIndex = index - 1
                val photo = photos[photoIndex]
                AsyncImage(
                    model = photo.thumbnailUrl.ifBlank { photo.imageUrl },
                    modifier = Modifier.clickable { onPhotoSelect(photo) },
                    placeholder = painterResource(R.drawable.cc_grayscale_pin),
                    contentDescription = photo.title,
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}