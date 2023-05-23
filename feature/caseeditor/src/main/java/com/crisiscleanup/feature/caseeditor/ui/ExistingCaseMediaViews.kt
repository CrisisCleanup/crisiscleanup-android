package com.crisiscleanup.feature.caseeditor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.designsystem.theme.primaryBlueOneTenthColor
import com.crisiscleanup.core.ui.touchDownConsumer
import com.crisiscleanup.feature.caseeditor.ExistingCaseViewModel
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.model.CaseImage

private val addMediaActionPadding = 4.dp
private val mediaCornerRadius = 6.dp
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
        cornerRadius = mediaCornerRadius.toPx()
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
        shape = RoundedCornerShape(mediaCornerRadius),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhotosSection(
    title: String,
    photoRowModifier: Modifier = Modifier,
    photoRowGridCells: StaggeredGridCells = StaggeredGridCells.Fixed(1),
    photos: List<CaseImage> = emptyList(),
    syncingWorksiteImage: Long = 0L,
    onAddPhoto: () -> Unit = {},
    onPhotoSelect: (CaseImage) -> Unit = {},
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
    // TODO Common styles
    val itemSpacing = 4.dp
    LazyHorizontalStaggeredGrid(
        modifier = photoRowModifier.touchDownConsumer { setEnableParentScroll(false) },
        rows = photoRowGridCells,
        state = gridState,
        horizontalItemSpacing = itemSpacing,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        items(
            1 + photos.size,
            key = {
                if (it == 0) "add-media"
                else photos[it - 1].imageUri
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
                        .clickable { onAddPhoto() }
                        .size(addActionSize),
                )
            } else {
                val photoIndex = index - 1
                val photo = photos[photoIndex]
                Box {
                    AsyncImage(
                        model = photo.thumbnailUri.ifBlank { photo.imageUri },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(mediaCornerRadius))
                            .clickable { onPhotoSelect(photo) },
                        placeholder = painterResource(R.drawable.cc_grayscale_pin),
                        contentDescription = photo.title,
                        contentScale = ContentScale.Crop,
                    )
                    if (syncingWorksiteImage == photo.id) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(CircleShape),
                            color = (Color.White.copy(alpha = 0.8f)),
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(16.dp),
                                imageVector = CrisisCleanupIcons.CloudSync,
                                contentDescription = stringResource(R.string.is_syncing),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TakePhotoSelectImage(
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    translate: (String) -> String = { s -> s },
    showOptions: Boolean = false,
    closeOptions: () -> Unit = {},
) {

    var cameraPhotoUri by remember { mutableStateOf(Uri.parse("")) }
    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isTaken ->
        if (isTaken) {
            viewModel.onMediaSelected(cameraPhotoUri)
        }
        closeOptions()
    }
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onMediaSelected(uri)
        }
        closeOptions()
    }

    if (showOptions) {
        ModalBottomSheet(
            onDismissRequest = closeOptions,
        ) {
            if (viewModel.hasCamera) {
                CrisisCleanupTextButton(
                    listItemModifier,
                    text = stringResource(R.string.take_photo),
                    onClick = {
                        if (viewModel.takePhoto()) {
                            val uri = viewModel.capturePhotoUri
                            if (uri == null) {
                                // TODO Show error message
                            } else {
                                cameraPhotoUri = uri
                                cameraPhotoLauncher.launch(cameraPhotoUri)
                            }
                        }
                    },
                )
            }
            CrisisCleanupTextButton(
                listItemModifier,
                text = stringResource(R.string.select_image),
                onClick = {
                    selectImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )
            Spacer(listItemModifier.padding(top = edgeSpacing))
        }
    }

    val closePermissionDialog =
        remember(viewModel) { { viewModel.showExplainPermissionCamera = false } }
    ExplainCameraPermissionDialog(
        showDialog = viewModel.showExplainPermissionCamera,
        closeDialog = closePermissionDialog,
        closeText = translate("actions.close"),
    )
}

@Composable
private fun ExplainCameraPermissionDialog(
    showDialog: Boolean,
    closeDialog: () -> Unit,
    closeText: String = "",
) {
    if (showDialog) {
        OpenSettingsDialog(
            R.string.allow_camera_permission,
            R.string.camera_permission_explanation,
            dismissText = closeText,
            closeDialog = closeDialog,
        )
    }
}