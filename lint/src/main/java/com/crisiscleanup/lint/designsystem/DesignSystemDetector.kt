package com.crisiscleanup.lint.designsystem

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression

/**
 * A detector that checks for incorrect usages of Compose Material APIs over equivalents in
 * the design system module.
 */
@Suppress("UnstableApiUsage")
class DesignSystemDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(
            UCallExpression::class.java,
            UQualifiedReferenceExpression::class.java
        )
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val name = node.methodName ?: return
                val preferredName = METHOD_NAMES[name] ?: return
                reportIssue(context, node, name, preferredName)
            }

            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                val name = node.receiver.asRenderString()
                val preferredName = RECEIVER_NAMES[name] ?: return
                reportIssue(context, node, name, preferredName)
            }
        }
    }

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "DesignSystem",
            briefDescription = "Design system",
            explanation = "This check highlights calls in code that use Compose Material " +
                    "composables instead of equivalents from the design system " +
                    "module.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(
                DesignSystemDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        // Unfortunately :lint is a Java module and thus can't depend on the :core-designsystem
        // Android module, so we can't use composable function references (eg. ::Button.name)
        // instead of hardcoded names.
        val METHOD_NAMES = mapOf(
            "MaterialTheme" to "CrisisCleanupTheme",
            "Button" to "CrisisCleanupButton",
            "OutlinedButton" to "CrisisCleanupOutlinedButton",
            "TextButton" to "CrisisCleanupTextButton",
            "FilterChip" to "CrisisCleanupFilterChip",
            "ElevatedFilterChip" to "CrisisCleanupFilterChip",
            "DropdownMenu" to "CrisisCleanupDropdownMenu",
            "NavigationBar" to "CrisisCleanupNavigationBar",
            "NavigationBarItem" to "CrisisCleanupNavigationBarItem",
            "NavigationRail" to "CrisisCleanupNavigationRail",
            "NavigationRailItem" to "CrisisCleanupNavigationRailItem",
            "TabRow" to "CrisisCleanupTabRow",
            "Tab" to "CrisisCleanupTab",
            "IconToggleButton" to "CrisisCleanupIconToggleButton",
            "FilledIconToggleButton" to "CrisisCleanupIconToggleButton",
            "FilledTonalIconToggleButton" to "CrisisCleanupIconToggleButton",
            "OutlinedIconToggleButton" to "CrisisCleanupIconToggleButton",
            "CenterAlignedTopAppBar" to "CrisisCleanupTopAppBar",
            "SmallTopAppBar" to "CrisisCleanupTopAppBar",
            "MediumTopAppBar" to "CrisisCleanupTopAppBar",
            "LargeTopAppBar" to "CrisisCleanupTopAppBar"
        )
        val RECEIVER_NAMES = mapOf(
            "Icons" to "CrisisCleanupIcons"
        )

        fun reportIssue(
            context: JavaContext,
            node: UElement,
            name: String,
            preferredName: String
        ) {
            context.report(
                ISSUE, node, context.getLocation(node),
                "Using $name instead of $preferredName"
            )
        }
    }
}
