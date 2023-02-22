package com.crisiscleanup.core.common.event

interface LogoutListener {
    suspend fun onLogout()
}

interface ExpiredTokenListener {
    fun onExpiredToken()
}