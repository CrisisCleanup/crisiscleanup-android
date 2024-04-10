package com.crisiscleanup.core.data.repository

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.database.dao.LocalImageDao
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.model.PopulatedWorksiteFiles
import com.crisiscleanup.core.database.model.toCaseImages
import com.crisiscleanup.core.model.data.CaseImage
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.WorksiteLocalImage
import com.crisiscleanup.core.model.data.asCaseImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

interface WorksiteImageRepository {
    val newPhotoUri: Uri?

    fun setUriFileAccessPermissions(uri: Uri)

    fun clearNewWorksiteImages()

    fun streamWorksiteImages(worksiteId: Long): Flow<List<CaseImage>>

    suspend fun queueWorksiteImage(
        worksiteId: Long,
        imageLocalUri: Uri,
        imageCategory: String,
    ): Boolean

    suspend fun transferNewWorksiteImages(worksiteId: Long)
}

@Singleton
class OfflineFirstWorksiteImageRepository @Inject constructor(
    private val worksiteDao: WorksiteDao,
    private val localImageDao: LocalImageDao,
    private val localImageRepository: LocalImageRepository,
    private val contentResolver: ContentResolver,
    @Logger(CrisisCleanupLoggers.Media) private val logger: AppLogger,
) : WorksiteImageRepository {
    private val newWorksiteImagesFlow = MutableStateFlow(emptyList<WorksiteLocalImage>())
    private val newWorksiteImagesLock = AtomicLong()

    override val newPhotoUri: Uri?
        @SuppressLint("SimpleDateFormat")
        get() {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = "CC_$timeStamp.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/CrisisCleanup")
            }
            return contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )
        }

    override fun setUriFileAccessPermissions(uri: Uri) {
        // TODO v29 fails to set access permission
        //      Must inform user file uploads will not work
        try {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flag)
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override fun clearNewWorksiteImages() {
        synchronized(newWorksiteImagesLock) {
            newWorksiteImagesFlow.value = emptyList()
        }
    }

    override fun streamWorksiteImages(worksiteId: Long): Flow<List<CaseImage>> {
        if (worksiteId == EmptyWorksite.id) {
            return newWorksiteImagesFlow.mapLatest {
                it.map(WorksiteLocalImage::asCaseImage)
            }
        }

        return worksiteDao.streamWorksiteFiles(worksiteId)
            .mapLatest(PopulatedWorksiteFiles::toCaseImages)
    }

    override suspend fun queueWorksiteImage(
        worksiteId: Long,
        imageLocalUri: Uri,
        imageCategory: String,
    ): Boolean {
        var displayName = ""
        val displayNameColumn = MediaStore.MediaColumns.DISPLAY_NAME
        val projection = arrayOf(displayNameColumn)
        contentResolver.query(imageLocalUri, projection, Bundle.EMPTY, null)?.let {
            it.use { cursor ->
                with(cursor) {
                    if (moveToFirst()) {
                        displayName = getString(getColumnIndexOrThrow(displayNameColumn))
                    }
                }
            }
        }

        if (displayName.isNotBlank()) {
            val localWorksiteImage = WorksiteLocalImage(
                0,
                worksiteId,
                documentId = displayName,
                uri = imageLocalUri.toString(),
                tag = imageCategory,
            )
            if (worksiteId == EmptyWorksite.id) {
                synchronized(newWorksiteImagesLock) {
                    newWorksiteImagesFlow.value =
                        newWorksiteImagesFlow.value.toMutableList().apply {
                            add(localWorksiteImage)
                        }
                }
            } else {
                localImageRepository.save(localWorksiteImage)
                return true
            }
        }

        return false
    }

    override suspend fun transferNewWorksiteImages(worksiteId: Long) {
        var images: List<WorksiteLocalImage> = emptyList()
        synchronized(newWorksiteImagesLock) {
            if (newWorksiteImagesLock.get() == worksiteId) {
                images = newWorksiteImagesFlow.value
            }
        }

        // TODO Save images as local images
        logger.logDebug("Transfer images to local $images")
//        val copyImages = images.map { it.copy(worksiteId = worksiteId) }
//        val entityImages = copyImages.map(WorksiteLocalImage::asEntity)
//        localImageDao.insertIgnore(entityImages)

        synchronized(newWorksiteImagesLock) {
            if (newWorksiteImagesLock.compareAndSet(worksiteId, 0L)) {
                newWorksiteImagesFlow.value = emptyList()
            }
        }
    }
}
