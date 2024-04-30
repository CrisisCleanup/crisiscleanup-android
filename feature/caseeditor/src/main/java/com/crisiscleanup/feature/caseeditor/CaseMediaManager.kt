package com.crisiscleanup.feature.caseeditor

import android.net.Uri
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.repository.LocalImageRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksiteImageRepository
import com.crisiscleanup.core.model.data.CaseImage
import com.crisiscleanup.core.model.data.EmptyWorksite
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal interface CaseCameraMediaManager {
    val hasCamera: Boolean
    val capturePhotoUri: Uri?
    val isCameraPermissionGranted: Boolean
    var showExplainPermissionCamera: Boolean

    fun onMediaSelected(uri: Uri, isFileSelected: Boolean)
    fun onMediaSelected(uris: List<Uri>)
    fun takePhoto(): Boolean
    fun continueTakePhoto(): Boolean
    fun onDeleteImage(image: CaseImage)
}

internal class CaseMediaManager(
    private val permissionManager: PermissionManager,
    private val localImageRepository: LocalImageRepository,
    private val worksiteImageRepository: WorksiteImageRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    private val viewModelScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: AppLogger,
) {
    val continueTakePhotoGate = AtomicBoolean(false)
    val isSavingMedia = MutableStateFlow(false)

    private val deletingImageIdsCache = mutableSetOf<Long>()
    val deletingImageIds = MutableStateFlow<Set<Long>>(emptySet())

    val syncingWorksiteImage = localImageRepository.syncingWorksiteImage
        .stateIn(
            scope = viewModelScope,
            initialValue = 0L,
            started = SharingStarted.WhileSubscribed(),
        )

    fun takePhoto(
        onShowExplainCameraPermission: () -> Unit,
    ): Boolean {
        when (permissionManager.requestCameraPermission()) {
            PermissionStatus.Granted -> {
                return true
            }

            PermissionStatus.ShowRationale -> {
                onShowExplainCameraPermission()
            }

            PermissionStatus.Requesting,
            PermissionStatus.Denied,
            PermissionStatus.Undefined,
            -> {
                // Ignore these statuses as they're not important
            }
        }
        return false
    }

    fun onMediaSelected(
        worksiteId: Long,
        addImageCategory: String,
        uri: Uri,
        isFileSelected: Boolean,
        onSaveFail: (Exception) -> Unit,
    ) {
        if (isFileSelected) {
            worksiteImageRepository.setUriFileAccessPermissions(uri)
        }

        isSavingMedia.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                val isQueued = worksiteImageRepository.queueWorksiteImage(
                    worksiteId,
                    uri,
                    addImageCategory,
                )
                if (isQueued && worksiteId != EmptyWorksite.id) {
                    syncPusher.scheduleSyncMedia()
                }
            } catch (e: Exception) {
                onSaveFail(e)
            } finally {
                isSavingMedia.value = false
            }
        }
    }

    fun deleteImage(
        imageId: Long,
        isNetworkImage: Boolean,
    ): Boolean {
        synchronized(deletingImageIdsCache) {
            if (deletingImageIds.value.contains(imageId)) {
                return false
            }
            deletingImageIdsCache.add(imageId)
            deletingImageIds.value = deletingImageIdsCache
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                if (isNetworkImage) {
                    val worksiteId = worksiteChangeRepository.saveDeletePhoto(imageId)
                    if (worksiteId > 0) {
                        syncPusher.appPushWorksite(worksiteId)
                    }
                } else {
                    localImageRepository.deleteLocalImage(imageId)
                }
            } catch (e: Exception) {
                // TODO Show error
                logger.logException(e)
            } finally {
                synchronized(deletingImageIdsCache) {
                    deletingImageIdsCache.remove(imageId)
                    deletingImageIds.value = deletingImageIdsCache
                }
            }
        }

        return true
    }
}
