package com.crisiscleanup.core.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.net.toUri
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.LocalImageDao
import com.crisiscleanup.core.database.dao.LocalImageDaoPlus
import com.crisiscleanup.core.database.dao.NetworkFileDao
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.WorksiteLocalImage
import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.model.NetworkFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

interface LocalImageRepository {
    val syncingWorksiteId: Flow<Long>
    val syncingWorksiteImage: Flow<Long>

    fun streamNetworkImageUrl(id: Long): Flow<String?>
    fun streamLocalImageUri(id: Long): Flow<String?>

    fun getImageRotation(id: Long, isNetworkImage: Boolean): Int

    suspend fun setImageRotation(id: Long, isNetworkImage: Boolean, rotationDegrees: Int)

    suspend fun save(image: WorksiteLocalImage)

    suspend fun deleteLocalImage(id: Long)

    suspend fun syncWorksiteMedia(worksiteId: Long): UploadMediaResult
}

class CrisisCleanupLocalImageRepository @Inject constructor(
    private val worksiteDao: WorksiteDao,
    private val networkFileDao: NetworkFileDao,
    private val localImageDao: LocalImageDao,
    private val localImageDaoPlus: LocalImageDaoPlus,
    private val contentResolver: ContentResolver,
    private val writeApi: CrisisCleanupWriteApi,
    private val syncLogger: SyncLogger,
    @ApplicationContext private val context: Context,
    @Logger(CrisisCleanupLoggers.Media) private val appLogger: AppLogger,
) : LocalImageRepository {
    private val fileUploadMutex = Mutex()

    override val syncingWorksiteId = MutableStateFlow(EmptyWorksite.id)
    override val syncingWorksiteImage = MutableStateFlow(0L)

    override fun streamNetworkImageUrl(id: Long) = networkFileDao.streamNetworkImageUrl(id)
    override fun streamLocalImageUri(id: Long) = localImageDao.streamLocalImageUri(id)

    override fun getImageRotation(id: Long, isNetworkImage: Boolean): Int {
        return if (isNetworkImage) {
            localImageDao.getNetworkFileLocalImage(id)?.rotateDegrees ?: 0
        } else {
            localImageDao.getLocalImage(id)?.rotateDegrees ?: 0
        }
    }

    override suspend fun setImageRotation(id: Long, isNetworkImage: Boolean, rotationDegrees: Int) {
        if (isNetworkImage) {
            localImageDaoPlus.setNetworkImageRotation(id, rotationDegrees)
        } else {
            localImageDaoPlus.setLocalImageRotation(id, rotationDegrees)
        }
    }

    override suspend fun save(image: WorksiteLocalImage) =
        localImageDaoPlus.upsertLocalImage(image.asEntity())

    override suspend fun deleteLocalImage(id: Long) = localImageDao.deleteLocalImage(id)

    private fun getFileNameType(uri: Uri): Pair<String, String> {
        var displayName = ""
        var mimeType = ""

        val displayNameColumn = MediaStore.MediaColumns.DISPLAY_NAME
        val mimeTypeColumn = MediaStore.MediaColumns.MIME_TYPE
        val projection = arrayOf(
            displayNameColumn,
            mimeTypeColumn,
        )
        contentResolver.query(uri, projection, Bundle.EMPTY, null)?.let {
            it.use { cursor ->
                with(cursor) {
                    if (moveToFirst()) {
                        displayName = getString(getColumnIndexOrThrow(displayNameColumn))
                        mimeType = getString(getColumnIndexOrThrow(mimeTypeColumn))
                    }
                }
            }
        }

        return Pair(displayName, mimeType)
    }

    private fun copyImageToFile(uri: Uri, fileName: String): File? {
        contentResolver.openFileDescriptor(uri, "r").use {
            it?.let { parcel ->
                val fileDescriptor = parcel.fileDescriptor
                FileInputStream(fileDescriptor).use { input ->
                    val outputFile = File(context.cacheDir, fileName)
                    Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    return outputFile
                }
            }
        }

        return null
    }

    private suspend fun uploadWorksiteFile(
        networkWorksiteId: Long,
        file: File,
        mimeType: String,
        imageTag: String,
    ): NetworkFile {
        val fileUpload = writeApi.startFileUpload(file.name, mimeType)
        with(fileUpload.uploadProperties) {
            writeApi.uploadFile(
                url,
                fields,
                file,
                mimeType,
            )
        }
        return writeApi.addFileToWorksite(networkWorksiteId, fileUpload.id, imageTag)
    }

    override suspend fun syncWorksiteMedia(worksiteId: Long): UploadMediaResult {
        val imagesPendingUpload = localImageDao.getWorksiteLocalImages(worksiteId)
        if (imagesPendingUpload.isEmpty()) {
            return UploadMediaResult(worksiteId)
        }

        val networkWorksiteId = worksiteDao.getWorksiteNetworkId(worksiteId)
        if (networkWorksiteId <= 0) {
            return UploadMediaResult(worksiteId)
        }

        syncLogger.type = "worksite-$worksiteId-media"

        syncLogger.log("Syncing ${imagesPendingUpload.size} images")

        val syncedImageIds = mutableSetOf<Long>()
        var deleteLogMessage = ""

        if (fileUploadMutex.tryLock()) {
            syncingWorksiteId.value = worksiteId
            try {
                for (localImage in imagesPendingUpload) {
                    val uri = localImage.uri.toUri()
                    if (uri.toString().isBlank()) {
                        deleteLogMessage = "Invalid URI ${localImage.uri}"
                    } else {
                        try {
                            val (fileName, mimeType) = getFileNameType(uri)
                            if (fileName.isBlank() || mimeType.isBlank()) {
                                deleteLogMessage = "File not found from ${localImage.uri}"
                            } else {
                                syncingWorksiteImage.value = localImage.id

                                val imageFile = copyImageToFile(uri, fileName)
                                if (imageFile == null) {
                                    syncLogger.log("Unable to copy image", localImage.uri)
                                } else {
                                    val networkFile = uploadWorksiteFile(
                                        networkWorksiteId,
                                        imageFile,
                                        mimeType,
                                        localImage.tag,
                                    )
                                    localImageDaoPlus.saveUploadedFile(
                                        worksiteId,
                                        localImage,
                                        networkFile.asEntity(),
                                    )
                                    syncedImageIds.add(localImage.id)
                                    syncLogger.log(
                                        "Synced ${localImage.id} (${networkFile.id} file ${networkFile.file})",
                                        "${syncedImageIds.size}/${imagesPendingUpload.size}",
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            appLogger.logException(e)
                            syncLogger.log("Sync error", e.message ?: "")
                        }
                    }

                    if (deleteLogMessage.isNotBlank()) {
                        syncLogger.log(
                            "Deleting image ${localImage.id}",
                            deleteLogMessage,
                        )
                        localImageDao.deleteLocalImage(localImage.id)
                    }
                }
            } finally {
                syncingWorksiteId.value = EmptyWorksite.id
                syncingWorksiteImage.value = 0
                fileUploadMutex.unlock()
            }
        }

        syncLogger.flush()

        val unsyncedIds = imagesPendingUpload.map { it.id }
            .filter { !syncedImageIds.contains(it) }
            .toSet()
        return UploadMediaResult(
            worksiteId,
            syncedImageIds = syncedImageIds,
            unsyncedImageIds = unsyncedIds,
        )
    }
}

data class UploadMediaResult(
    val worksiteId: Long,
    val syncedImageIds: Set<Long> = emptySet(),
    val unsyncedImageIds: Set<Long> = emptySet(),
)
