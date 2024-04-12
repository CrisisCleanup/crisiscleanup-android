package com.crisiscleanup.feature.caseeditor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import com.crisiscleanup.core.appnav.ViewImageArgs
import com.crisiscleanup.core.common.urlEncode
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.OpenSettingsDialog
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.designsystem.theme.primaryBlueOneTenthColor
import com.crisiscleanup.core.model.data.CaseImage
import com.crisiscleanup.core.model.data.ImageCategory
import com.crisiscleanup.core.ui.touchDownConsumer
import com.crisiscleanup.feature.caseeditor.CaseCameraMediaManager
import com.crisiscleanup.feature.caseeditor.R

private val addMediaActionPadding = 4.dp
private val mediaCornerRadius = 6.dp
private val addMediaStrokeWidth = 2.dp
private val addMediaStrokeDash = 6.dp
private val addMediaStrokeGap = 6.dp

@Composable
internal fun CasePhotoImageView(
    cameraMediaManager: CaseCameraMediaManager,
    setEnablePagerScroll: (Boolean) -> Unit,
    photos: Map<ImageCategory, List<CaseImage>>,
    syncingWorksiteImage: Long,
    deletingImageIds: Set<Long>,
    onUpdateImageCategory: (ImageCategory) -> Unit,
    viewHeaderTitle: String,
    maxHeight: Dp = 480.dp,
    onPhotoSelect: (ViewImageArgs) -> Unit = { _ -> },
) {
    var showCameraMediaSelect by remember { mutableStateOf(false) }

    val t = LocalAppTranslator.current
    val sectionTitleResIds = mapOf(
        ImageCategory.Before to t("caseForm.before_photos"),
        ImageCategory.After to t("caseForm.after_photos"),
    )
    var isShortScreen by remember { mutableStateOf(false) }
    val onDeleteImage = remember(cameraMediaManager) {
        { image: CaseImage ->
            cameraMediaManager.onDeleteImage(image)
        }
    }
    Column(
        modifier = Modifier
            .sizeIn(maxHeight = maxHeight)
            .fillMaxHeight()
            .onGloballyPositioned {
                isShortScreen = it.size.height.dp < 720.dp
            },
    ) {
        sectionTitleResIds.onEach { (imageCategory, sectionTitle) ->
            photos[imageCategory]?.let { rowPhotos ->
                val title = if (isShortScreen) {
                    sectionTitle.replace(" ", "\n")
                } else {
                    sectionTitle
                }
                PhotosSection(
                    title,
                    Modifier
                        .listItemVerticalPadding()
                        .weight(0.45f),
                    StaggeredGridCells.Fixed(1),
                    isShortScreen,
                    photos = rowPhotos,
                    deletingImageIds = deletingImageIds,
                    setEnableParentScroll = setEnablePagerScroll,
                    onAddPhoto = {
                        onUpdateImageCategory(imageCategory)
                        showCameraMediaSelect = true
                    },
                    onDeleteImage = onDeleteImage,
                    onPhotoSelect = { image: CaseImage ->
                        with(image) {
                            val viewImageArgs = ViewImageArgs(
                                id,
                                encodedUri = if (isNetworkImage) imageUri.urlEncode() else "",
                                isNetworkImage,
                                viewHeaderTitle.urlEncode(),
                            )
                            onPhotoSelect(viewImageArgs)
                        }
                    },
                    syncingWorksiteImage = syncingWorksiteImage,
                )
            }
        }
    }

    val closeCameraMediaSelect = remember(cameraMediaManager) { { showCameraMediaSelect = false } }
    TakePhotoSelectImage(
        cameraMediaManager,
        showCameraMediaSelect,
        closeCameraMediaSelect,
    )
}

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
private fun SurfaceIcon(
    imageVector: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    iconPadding: Dp = 16.dp,
    contentDescription: String? = null,
) {
    Surface(
        modifier = modifier.clip(CircleShape),
        color = color,
    ) {
        Icon(
            modifier = Modifier
                .size(iconSize)
                .padding(iconPadding),
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

@Composable
internal fun PhotosSection(
    title: String,
    modifier: Modifier = Modifier,
    photoRowGridCells: StaggeredGridCells = StaggeredGridCells.Fixed(1),
    isInlineContent: Boolean = false,
    photos: List<CaseImage> = emptyList(),
    syncingWorksiteImage: Long = 0L,
    deletingImageIds: Set<Long> = emptySet(),
    onAddPhoto: () -> Unit = {},
    onDeleteImage: (CaseImage) -> Unit = {},
    onPhotoSelect: (CaseImage) -> Unit = {},
    setEnableParentScroll: (Boolean) -> Unit = {},
    addActionSize: Dp = 128.dp,
) {
    if (!isInlineContent) {
        Text(
            title,
            listItemModifier,
            style = LocalFontStyles.current.header4,
        )
    }

    val gridState = rememberLazyStaggeredGridState()
    LaunchedEffect(gridState.isScrollInProgress) {
        if (!gridState.isScrollInProgress) {
            setEnableParentScroll(true)
        }
    }
    // TODO Common styles
    val inlineContentCount = if (isInlineContent) 1 else 0
    val itemSpacing = 4.dp
    LazyHorizontalStaggeredGrid(
        modifier = modifier.touchDownConsumer { setEnableParentScroll(false) },
        rows = photoRowGridCells,
        state = gridState,
        horizontalItemSpacing = itemSpacing,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        val categoryTitleIndex = if (isInlineContent) 0 else -99
        val addMediaIndex = if (isInlineContent) 1 else 0
        val photoOffsetIndex = addMediaIndex + 1
        val totalCount = inlineContentCount + 1 + photos.size + 1
        val endSpaceIndex = totalCount - 1
        items(
            totalCount,
            key = {
                when (it) {
                    categoryTitleIndex -> "category-title"
                    addMediaIndex -> "add-media"
                    endSpaceIndex -> "end-spacer"
                    else -> photos[it - photoOffsetIndex].imageUri
                }
            },
            contentType = {
                when (it) {
                    categoryTitleIndex -> "category-title"
                    addMediaIndex -> "add-media"
                    endSpaceIndex -> "end-spacer"
                    else -> "photo-image"
                }
            },
        ) { index ->
            when (index) {
                categoryTitleIndex -> {
                    Text(
                        title,
                        listItemModifier,
                        style = LocalFontStyles.current.header4,
                    )
                }

                addMediaIndex -> {
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
                    val t = LocalAppTranslator.current
                    val photoIndex = index - photoOffsetIndex
                    val photo = photos[photoIndex]
                    val isSyncing = syncingWorksiteImage != 0L && syncingWorksiteImage == photo.id
                    val isDeleting = deletingImageIds.contains(photo.id)
                    val halfWhite = Color.White.copy(alpha = 0.5f)

                    var contentSize by remember { mutableStateOf(Size.Zero) }
                    Box(
                        Modifier.onGloballyPositioned {
                            contentSize = it.size.toSize()
                        },
                    ) {
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
                        // TODO Common dimensions where defined
                        val isTransient = isSyncing || isDeleting
                        if (isTransient) {
                            SurfaceIcon(
                                CrisisCleanupIcons.CloudSync,
                                Color.White.copy(alpha = 0.8f),
                                Modifier
                                    .align(Alignment.Center)
                                    .clip(CircleShape),
                                iconSize = 64.dp,
                                contentDescription = t("info.is_syncing"),
                            )
                        } else if (!photo.isNetworkImage) {
                            SurfaceIcon(
                                CrisisCleanupIcons.Cloud,
                                halfWhite,
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .padding(end = 36.dp),
                                iconPadding = 4.dp,
                                contentDescription = t("info.awaiting_cloud_sync"),
                            )
                        }

                        if (!isTransient) {
                            val onDelete = remember(photo) { { onDeleteImage(photo) } }
                            RowImageContextMenu(
                                contentSize,
                                halfWhite,
                                onDelete = onDelete,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.RowImageContextMenu(
    contentSize: Size,
    actionBackgroundColor: Color,
    enabled: Boolean = true,
    onDelete: () -> Unit = {},
) {
    val t = LocalAppTranslator.current
    var showDropdown by remember { mutableStateOf(false) }
    Box(
        Modifier
            .align(Alignment.TopEnd),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .offset(x = 4.dp, y = (-4).dp)
                .clip(CircleShape)
                .clickable(
                    enabled = enabled,
                    onClick = { showDropdown = !showDropdown },
                ),
        ) {
            SurfaceIcon(
                CrisisCleanupIcons.MoreVert,
                actionBackgroundColor,
                Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
                iconPadding = 4.dp,
                contentDescription = t("actions.show_more"),
            )
        }

        if (showDropdown) {
            DropdownMenu(
                modifier = Modifier
                    .width(
                        with(LocalDensity.current) {
                            contentSize.width.toDp()
                        },
                    ),
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
            ) {
                DropdownMenuItem(
                    modifier = Modifier.optionItemHeight(),
                    text = { Text(t("actions.delete")) },
                    onClick = {
                        onDelete()
                        showDropdown = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TakePhotoSelectImage(
    cameraMediaManager: CaseCameraMediaManager,
    showOptions: Boolean = false,
    closeOptions: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current
    var cameraPhotoUri by remember { mutableStateOf(Uri.parse("")) }
    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { isTaken ->
        if (isTaken) {
            cameraMediaManager.onMediaSelected(cameraPhotoUri, false)
        }
        closeOptions()
    }
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris: List<Uri> ->
        cameraMediaManager.onMediaSelected(uris)
        closeOptions()
    }

    if (showOptions) {
        ModalBottomSheet(
            onDismissRequest = closeOptions,
        ) {
            if (cameraMediaManager.hasCamera) {
                val continueTakePhoto = remember(cameraMediaManager) {
                    {
                        val uri = cameraMediaManager.capturePhotoUri
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
                        if (cameraMediaManager.takePhoto()) {
                            continueTakePhoto()
                        }
                    },
                )

                if (cameraMediaManager.isCameraPermissionGranted && cameraMediaManager.continueTakePhoto()) {
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
        remember(cameraMediaManager) { { cameraMediaManager.showExplainPermissionCamera = false } }
    ExplainCameraPermissionDialog(
        showDialog = cameraMediaManager.showExplainPermissionCamera,
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
