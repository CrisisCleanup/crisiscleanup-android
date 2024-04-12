package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.model.data.CaseImage
import com.crisiscleanup.core.model.data.ImageCategory
import com.crisiscleanup.core.model.data.NetworkImage
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteLocalImage
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.core.model.data.asCaseImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

internal fun processWorksiteFilesNotes(
    editableWorksite: Flow<Worksite>,
    viewState: Flow<CaseEditorViewState>,
) = combine(
    editableWorksite,
    viewState,
    ::Pair,
)
    .filter { (_, state) -> state is CaseEditorViewState.CaseData }
    .mapLatest { (worksite, state) ->
        val fileImages = worksite.files.map(NetworkImage::asCaseImage)
        val localImages = (state as CaseEditorViewState.CaseData).localWorksite
            ?.localImages
            ?.map(WorksiteLocalImage::asCaseImage)
            ?: emptyList()
        CaseImagesNotes(fileImages, localImages, worksite.notes)
    }

internal fun organizeBeforeAfterPhotos(
    filesNotes: Flow<CaseImagesNotes>,
) = filesNotes
    .mapLatest { (files, localFiles) ->
        val beforeImages = localFiles.filterNot(CaseImage::isAfter).toMutableList()
            .apply { addAll(files.filterNot(CaseImage::isAfter)) }
        val afterImages = localFiles.filter(CaseImage::isAfter).toMutableList()
            .apply { addAll(files.filter(CaseImage::isAfter)) }
        mapOf(
            ImageCategory.Before to beforeImages,
            ImageCategory.After to afterImages,
        )
    }

internal data class CaseImagesNotes(
    val networkImages: List<CaseImage>,
    val localImages: List<CaseImage>,
    val notes: List<WorksiteNote>,
)

internal fun Flow<List<CaseImage>>.mapToCategoryLookup() = map { images ->
    val lookup = mutableMapOf(
        ImageCategory.Before to mutableListOf<CaseImage>(),
        ImageCategory.After to mutableListOf(),
    )
    images.forEach { image ->
        val category = if (image.tag == ImageCategory.Before.literal) {
            ImageCategory.Before
        } else {
            ImageCategory.After
        }
        lookup[category]!!.add(image)
    }
    lookup
}
