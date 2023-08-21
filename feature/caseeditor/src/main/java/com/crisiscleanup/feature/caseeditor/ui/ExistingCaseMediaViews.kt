package com.crisiscleanup.feature.caseeditor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.OpenSettingsDialog
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
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
    val text = LocalAppTranslator.current("actions.add_media")
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
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeDash, strokeGap), 0f),
    )
    Surface(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = contentColor,
                    cornerRadius = CornerRadius(cornerRadius),
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
                style = LocalFontStyles.current.header5,
                textAlign = TextAlign.Center,
                color = contentColor,
            )
        }
    }
}

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
        style = LocalFontStyles.current.header4,
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
        val totalCount = 1 + photos.size + 1
        val endSpaceIndex = totalCount - 1
        items(
            totalCount,
            key = {
                when (it) {
                    0 -> "add-media"
                    endSpaceIndex -> "end-spacer"
                    else -> photos[it - 1].imageUri
                }
            },
            contentType = {
                when (it) {
                    0 -> "add-media"
                    endSpaceIndex -> "end-spacer"
                    else -> "photo-image"
                }
            },
        ) { index ->
            when (index) {
                0 -> {
                    AddMediaView(
                        Modifier
                            .padding(addMediaActionPadding)
                            .clickable { onAddPhoto() }
                            .size(addActionSize),
                    )
                }

                endSpaceIndex -> {
                    Spacer(modifier = Modifier.width(0.1.dp))
                }

                else -> {
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
                        // TODO Common dimensions
                        val translator = LocalAppTranslator.current
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
                                    contentDescription = translator("info.is_syncing"),
                                )
                            }
                        } else if (!photo.isNetworkImage) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape),
                                color = (Color.White.copy(alpha = 0.5f)),
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(4.dp),
                                    imageVector = CrisisCleanupIcons.Cloud,
                                    contentDescription = translator("~~Awaiting cloud sync"),
                                )
                            }
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
    showOptions: Boolean = false,
    closeOptions: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current
    var cameraPhotoUri by remember { mutableStateOf(Uri.parse("")) }
    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { isTaken ->
        if (isTaken) {
            viewModel.onMediaSelected(cameraPhotoUri, false)
        }
        closeOptions()
    }
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris: List<Uri> ->
        viewModel.onMediaSelected(uris)
        closeOptions()
    }

    if (showOptions) {
        ModalBottomSheet(
            onDismissRequest = closeOptions,
        ) {
            if (viewModel.hasCamera) {
                val continueTakePhoto = remember(viewModel) {
                    {
                        val uri = viewModel.capturePhotoUri
                        if (uri == null) {
                            // TODO Show error message
                        } else {
                            cameraPhotoUri = uri
                            cameraPhotoLauncher.launch(cameraPhotoUri)
                        }
                    }
                }

                CrisisCleanupTextButton(
                    listItemModifier,
                    text = translator("actions.take_photo"),
                    onClick = {
                        if (viewModel.takePhoto()) {
                            continueTakePhoto()
                        }
                    },
                )

                if (viewModel.isCameraPermissionGranted && viewModel.continueTakePhoto()) {
                    continueTakePhoto()
                }
            }
            CrisisCleanupTextButton(
                listItemModifier,
                text = translator("fileUpload.select_file_upload"),
                onClick = {
                    selectImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
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
        closeText = translator("actions.close"),
    )
}

@Composable
private fun ExplainCameraPermissionDialog(
    showDialog: Boolean,
    closeDialog: () -> Unit,
    closeText: String = "",
) {
    if (showDialog) {
        val t = LocalAppTranslator.current
        OpenSettingsDialog(
            t("info.allow_access_to_camera"),
            t("info.explain_allow_access_to_camera_android"),
            confirmText = t("info.app_settings"),
            dismissText = closeText,
            closeDialog = closeDialog,
        )
    }
}
