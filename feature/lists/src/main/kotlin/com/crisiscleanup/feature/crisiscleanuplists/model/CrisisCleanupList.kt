package com.crisiscleanup.feature.crisiscleanuplists.model

import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.icon.Icon
import com.crisiscleanup.core.designsystem.icon.Icon.DrawableResourceIcon
import com.crisiscleanup.core.designsystem.icon.Icon.ImageVectorIcon
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.ListModel
import com.crisiscleanup.core.commonassets.R as commonassetsR

private val modelIconLookup = mapOf(
    ListModel.None to ImageVectorIcon(CrisisCleanupIcons.Warning),
    ListModel.File to ImageVectorIcon(CrisisCleanupIcons.File),
    ListModel.Incident to DrawableResourceIcon(commonassetsR.drawable.ic_tornado_line),
    ListModel.List to ImageVectorIcon(CrisisCleanupIcons.List),
    ListModel.Organization to ImageVectorIcon(CrisisCleanupIcons.Organization),
    ListModel.OrganizationIncidentTeam to DrawableResourceIcon(CrisisCleanupIcons.Team),
    ListModel.User to ImageVectorIcon(CrisisCleanupIcons.Person),
    ListModel.Worksite to DrawableResourceIcon(CrisisCleanupIcons.Cases),
)

val CrisisCleanupList.ListIcon: Icon
    get() = modelIconLookup[model]!!
