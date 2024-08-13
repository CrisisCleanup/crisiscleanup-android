package com.crisiscleanup

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.crisiscleanup.core.model.data.TutorialViewId
import com.crisiscleanup.core.model.data.TutorialViewId.AccountToggle
import com.crisiscleanup.core.model.data.TutorialViewId.AppNavBar
import com.crisiscleanup.core.model.data.TutorialViewId.IncidentSelectDropdown
import com.crisiscleanup.core.ui.LayoutSizePosition
import com.crisiscleanup.core.ui.TutorialViewTracker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupTutorialViewTracker @Inject constructor(
) : TutorialViewTracker {
    override val viewSizePositionLookup =
        SnapshotStateMap<TutorialViewId, LayoutSizePosition>().also {
            it[AppNavBar] = LayoutSizePosition()
            it[IncidentSelectDropdown] = LayoutSizePosition()
            it[AccountToggle] = LayoutSizePosition()
        }
}