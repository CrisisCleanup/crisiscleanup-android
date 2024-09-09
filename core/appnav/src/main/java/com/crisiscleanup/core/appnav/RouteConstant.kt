package com.crisiscleanup.core.appnav

object RouteConstant {
    const val CASES_GRAPH_ROUTE = "cases_graph"
    const val AUTH_GRAPH_ROUTE = "auth_graph"

    const val AUTH_ROUTE = "auth_route"
    const val LOGIN_WITH_EMAIL_ROUTE = "$AUTH_ROUTE/login_with_email"
    const val LOGIN_WITH_PHONE_ROUTE = "$AUTH_ROUTE/login_with_phone"
    const val FORGOT_PASSWORD_ROUTE = "forgot_password_route"
    const val EMAIL_LOGIN_LINK_ROUTE = "email_login_link_route"
    const val AUTH_RESET_PASSWORD_ROUTE = "$AUTH_ROUTE/auth_reset_password_route"
    const val MAGIC_LINK_ROUTE = "$AUTH_ROUTE/magic_link_login"

    const val REQUEST_ACCESS_ROUTE = "$AUTH_ROUTE/request_access"
    const val ORG_PERSISTENT_INVITE_ROUTE = "$AUTH_ROUTE/org_persistent_invite"

    const val VOLUNTEER_ORG_ROUTE = "$AUTH_ROUTE/volunteer_org"
    const val VOLUNTEER_PASTE_INVITE_LINK_ROUTE = "$VOLUNTEER_ORG_ROUTE/paste_invitation_link"
    const val VOLUNTEER_REQUEST_ACCESS_ROUTE = "$VOLUNTEER_ORG_ROUTE/request_access"
    const val VOLUNTEER_SCAN_QR_CODE_ROUTE = "$VOLUNTEER_ORG_ROUTE/scan_qr_code"

    const val CASES_ROUTE = "cases_route"
    const val DASHBOARD_ROUTE = "dashboard_route"
    const val MENU_ROUTE = "menu_route"
    const val TEAM_ROUTE = "team_route"
    val topLevelRoutes = setOf(CASES_ROUTE, TEAM_ROUTE, MENU_ROUTE)

    const val INVITE_TEAMMATE_ROUTE = "invite_teammate"
    const val REQUEST_REDEPLOY_ROUTE = "request_redeploy"
    const val USER_FEEDBACK_ROUTE = "user_feedback_route"

    const val SYNC_INSIGHTS_ROUTE = "sync_insights_route"

    const val CASES_SEARCH_ROUTE = "cases_search"
    const val CASES_FILTER_ROUTE = "cases_filter"

    const val CASE_EDITOR_ROUTE = "case_editor"
    const val CASE_EDITOR_SEARCH_ADDRESS_ROUTE = "$CASE_EDITOR_ROUTE/edit_search_address"
    const val CASE_EDITOR_MAP_MOVE_LOCATION_ROUTE = "$CASE_EDITOR_ROUTE/edit_map_move_location"

    const val VIEW_CASE_ROUTE = "view_case"
    const val VIEW_CASE_TRANSFER_WORK_TYPES_ROUTE = "$VIEW_CASE_ROUTE/transfer_work_types"
    const val CASE_ADD_FLAG_ROUTE = "$VIEW_CASE_ROUTE/add_flag"
    const val CASE_SHARE_ROUTE = "$VIEW_CASE_ROUTE/share_case"
    const val CASE_HISTORY_ROUTE = "$VIEW_CASE_ROUTE/case_history"

    const val VIEW_IMAGE_ROUTE = "view_image"
    const val WORKSITE_IMAGES_ROUTE = "worksite_images"

    const val ACCOUNT_RESET_PASSWORD_ROUTE = "account_reset_password_route"

    const val LISTS_ROUTE = "crisis_cleanup_lists"
    const val VIEW_LIST_ROUTE = "view_list"

    const val VIEW_TEAM_ROUTE = "view_team"
    const val TEAM_EDITOR_ROUTE = "team_editor"
}
