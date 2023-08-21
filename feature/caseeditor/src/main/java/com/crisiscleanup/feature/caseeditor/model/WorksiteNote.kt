package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.model.data.WorksiteNote

fun WorksiteNote.getRelativeDate() = createdAt.relativeTime
