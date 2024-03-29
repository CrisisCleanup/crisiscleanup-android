package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.model.data.InitiatePhoneLoginResult
import com.crisiscleanup.core.model.data.PasswordResetInitiation
import com.crisiscleanup.core.network.CrisisCleanupAccountApi
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import javax.inject.Inject

interface AccountUpdateRepository {
    suspend fun initiateEmailMagicLink(emailAddress: String): Boolean
    suspend fun initiatePhoneLogin(phoneNumber: String): InitiatePhoneLoginResult
    suspend fun initiatePasswordReset(emailAddress: String): PasswordResetInitiation
    suspend fun changePassword(password: String, token: String): Boolean
    suspend fun acceptTerms(): Boolean
}

class CrisisCleanupAccountUpdateRepository @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val accountApi: CrisisCleanupAccountApi,
    @Logger(CrisisCleanupLoggers.Account) private val logger: AppLogger,
) : AccountUpdateRepository {
    override suspend fun initiateEmailMagicLink(emailAddress: String): Boolean {
        try {
            return accountApi.initiateMagicLink(emailAddress)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return false
    }

    override suspend fun initiatePhoneLogin(phoneNumber: String): InitiatePhoneLoginResult {
        try {
            return accountApi.initiatePhoneLogin(phoneNumber)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return InitiatePhoneLoginResult.Unknown
    }

    override suspend fun initiatePasswordReset(emailAddress: String): PasswordResetInitiation {
        try {
            val result = accountApi.initiatePasswordReset(emailAddress)
            if (result.isValid && result.expiresAt > Clock.System.now()) {
                return PasswordResetInitiation(result.expiresAt, "")
            } else {
                if (result.invalidMessage?.isNotBlank() == true) {
                    return PasswordResetInitiation(null, result.invalidMessage!!)
                }
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
        return PasswordResetInitiation(null, "")
    }

    override suspend fun changePassword(password: String, token: String): Boolean {
        try {
            return accountApi.changePassword(password, token)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return false
    }

    override suspend fun acceptTerms(): Boolean {
        try {
            val userId = accountDataRepository.accountData.first().id
            return accountApi.acceptTerms(userId)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return false
    }
}
