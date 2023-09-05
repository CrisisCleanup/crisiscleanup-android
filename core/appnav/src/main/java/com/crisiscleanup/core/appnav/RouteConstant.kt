package com.crisiscleanup.core.appnav

object RouteConstant {
    const val casesGraphRoutePattern = "cases_graph"
    const val authGraphRoutePattern = "auth_graph"

    const val authRoute = "auth_route"
    const val loginWithEmailRoute = "$authRoute/login_with_email"
    const val survivorInfoRoute = "$authRoute/survivor_info"
    const val forgotPasswordRoute = "forgot_password_route"
    const val emailLoginLinkRoute = "email_login_link_route"
    // const val resetPasswordRoute = "reset_password_route"

    // This cannot be used as the navHost startDestination
    const val casesRoute = "cases_route"
    const val dashboardRoute = "dashboard_route"
    const val menuRoute = "menu_route"
    const val teamRoute = "team_route"
    val topLevelRoutes = setOf(casesRoute, menuRoute)

    const val userFeedbackRoute = "user_feedback_route"

    const val syncInsightsRoute = "sync_insights_route"

    const val casesSearchRoute = "cases_search"
    const val casesFilterRoute = "cases_filter"

    const val caseEditorRoute = "case_editor"
    const val caseEditSearchAddressRoute = "$caseEditorRoute/edit_search_address"
    const val caseEditMapMoveLocationRoute = "$caseEditorRoute/edit_map_move_location"

    const val viewCaseRoute = "view_case"
    const val viewCaseTransferWorkTypesRoute = "$viewCaseRoute/transfer_work_types"
    const val caseAddFlagRoute = "$viewCaseRoute/add_flag"
    const val caseShareRoute = "$viewCaseRoute/share_case"
    const val caseHistoryRoute = "$viewCaseRoute/case_history"

    const val viewImageRoute = "view_image"
}
