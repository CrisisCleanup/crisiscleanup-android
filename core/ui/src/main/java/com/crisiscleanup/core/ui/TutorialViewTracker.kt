package com.crisiscleanup.core.ui

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.crisiscleanup.core.model.data.TutorialViewId

interface TutorialViewTracker {
    val viewSizePositionLookup: SnapshotStateMap<TutorialViewId, LayoutSizePosition>
}