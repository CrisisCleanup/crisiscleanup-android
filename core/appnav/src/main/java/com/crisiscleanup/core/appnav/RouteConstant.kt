package com.crisiscleanup.core.appnav

object RouteConstant {
    const val casesGraphRoutePattern = "cases_graph"

    // This cannot be used as the navHost startDestination
    const val casesRoute = "cases_route"
    const val dashboardRoute = "dashboard_route"
    const val menuRoute = "menu_route"
    const val teamRoute = "team_route"
    val topLevelRoutes = setOf(
        casesRoute,
        dashboardRoute,
        menuRoute,
        teamRoute,
    )

    const val settingsRoute = "settings_route"

    const val selectIncidentRoute = "select_incident"

    const val caseEditorRoute = "case_editor"
    const val caseEditPropertyRoute = "case_editor/edit_property"
}
