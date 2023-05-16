package com.crisiscleanup.core.appnav

object RouteConstant {
    const val casesGraphRoutePattern = "cases_graph"

    // This cannot be used as the navHost startDestination
    const val casesRoute = "cases_route"
    const val dashboardRoute = "dashboard_route"
    const val menuRoute = "menu_route"
    const val teamRoute = "team_route"

    const val settingsRoute = "settings_route"

    const val syncInsightsRoute = "sync_insights_route"

    const val selectIncidentRoute = "select_incident"

    const val casesSearchRoute = "cases_search"

    const val caseEditorRoute = "case_editor"
    const val caseEditSearchAddressRoute = "$caseEditorRoute/edit_search_address"
    const val caseEditMapMoveLocationRoute = "$caseEditorRoute/edit_map_move_location"

    const val viewCaseRoute = "view_case"
    const val viewCaseTransferWorkTypesRoute = "$viewCaseRoute/transfer_work_types"

    const val viewImageRoute = "view_image"

    val fullscreenRoutes = setOf(
        casesSearchRoute,
        caseEditorRoute,
        syncInsightsRoute,
    )
}
