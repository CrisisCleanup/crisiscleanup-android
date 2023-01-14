package com.crisiscleanup.core.network

import javax.inject.Inject
import javax.inject.Singleton

/*
 * TODO Research a better pattern/design. Or develop a better.
 *      A manager should not
 */
/**
 * Manages live access tokens for synchronous access like setting in request headers
 *
 * This manager must keep this value up to date at all times and clear it once it is not applicable.
 */
interface AccessTokenManager {
    var accessToken: String
}

@Singleton
class SimpleAccessTokenManager @Inject constructor() : AccessTokenManager {
    override var accessToken: String = ""
}