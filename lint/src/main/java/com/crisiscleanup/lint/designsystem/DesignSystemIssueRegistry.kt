package com.crisiscleanup.lint.designsystem

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

/**
 * An issue registry that checks for incorrect usages of Compose Material APIs over equivalents in
 * the design system module.
 */
@Suppress("UnstableApiUsage")
class DesignSystemIssueRegistry : IssueRegistry() {
    override val issues = listOf(DesignSystemDetector.ISSUE)

    override val api: Int = CURRENT_API

    override val minApi: Int = 29

    override val vendor: Vendor = Vendor(
        vendorName = "Crisis Cleanup",
        // TODO Update once repo is created
        feedbackUrl = "https://github.com/CrisisCleanup/crisis-cleanup-android/issues",
        contact = "https://github.com/CrisisCleanup/crisis-cleanup-android"
    )
}
