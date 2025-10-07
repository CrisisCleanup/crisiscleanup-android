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
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.LocalImageDaoPlus
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
import javax.inject.Inject
import javax.inject.Singleton

interface WorksiteImageRepository {
    val newPhotoUri: Uri?

    val hasNewWorksiteImages: Boolean

    fun setUriFileAccessPermissions(uri: Uri)

    fun clearNewWorksiteImages()

    fun streamNewWorksiteImages(): Flow<List<CaseImage>>
    fun streamWorksiteImages(worksiteId: Long): Flow<List<CaseImage>>

    suspend fun queueWorksiteImage(
        worksiteId: Long,
        imageLocalUri: Uri,
        imageCategory: String,
    ): Boolean

    fun deleteNewWorksiteImage(uri: String)
    suspend fun transferNewWorksiteImages(worksiteId: Long)
}

@Singleton
class OfflineFirstWorksiteImageRepository @Inject constructor(
    private val worksiteDao: WorksiteDao,
    private val localImageDaoPlus: LocalImageDaoPlus,
    private val localImageRepository: LocalImageRepository,
    private val contentResolver: ContentResolver,
    @Logger(CrisisCleanupLoggers.Media) private val logger: AppLogger,
) : WorksiteImageRepository {
    private val newWorksiteImagesFlow = MutableStateFlow(emptyList<WorksiteLocalImage>())
    private var newWorksiteImagesCache = mutableMapOf<String, WorksiteLocalImage>()
    private val newWorksiteImagesLock = Object()

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

    override val hasNewWorksiteImages: Boolean
        get() = newWorksiteImagesCache.isNotEmpty()

    override fun setUriFileAccessPermissions(uri: Uri) {
        // TODO API 29 (v10) fails to set access permission
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
            newWorksiteImagesCache = mutableMapOf()
            newWorksiteImagesFlow.value = emptyList()
        }
    }

    override fun streamNewWorksiteImages() = newWorksiteImagesFlow.mapLatest {
        it.map(WorksiteLocalImage::asCaseImage)
    }

    override fun streamWorksiteImages(worksiteId: Long) =
        worksiteDao.streamWorksiteFiles(worksiteId)
            .mapLatest(PopulatedWorksiteFiles::toCaseImages)

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
                    newWorksiteImagesCache[localWorksiteImage.uri] = localWorksiteImage
                    newWorksiteImagesFlow.value = newWorksiteImagesCache.values.toList()
                }
            } else {
                localImageRepository.save(localWorksiteImage)
            }
            return true
        }

        return false
    }

    override fun deleteNewWorksiteImage(uri: String) {
        synchronized(newWorksiteImagesLock) {
            newWorksiteImagesCache.remove(uri)?.let {
                newWorksiteImagesFlow.value = newWorksiteImagesCache.values.toList()
            }
        }
    }

    override suspend fun transferNewWorksiteImages(worksiteId: Long) {
        var images: List<WorksiteLocalImage>
        synchronized(newWorksiteImagesLock) {
            images = newWorksiteImagesCache.values.toList()
        }

        try {
            val copyImages = images.map { it.copy(worksiteId = worksiteId) }
            val entityImages = copyImages.map(WorksiteLocalImage::asEntity)
            // TODO Write test
            localImageDaoPlus.upsertLocalImages(entityImages)

            clearNewWorksiteImages()
        } catch (e: Exception) {
            logger.logException(e)
        }
    }
}
