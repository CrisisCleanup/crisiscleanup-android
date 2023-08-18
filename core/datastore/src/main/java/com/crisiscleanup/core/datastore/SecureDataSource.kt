package com.crisiscleanup.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SecureDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val sharedPrefsFile = "crisis-cleanup-secure-prefs"
    private val sharedPreferences: SharedPreferences

    private val refreshTokenKey = "auth-refresh-token"
    private val accessTokenKey = "auth-access-token"

    init {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)

        sharedPreferences = EncryptedSharedPreferences.create(
            sharedPrefsFile,
            mainKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveAuthTokens(refreshToken: String, accessToken: String) {
        with(sharedPreferences.edit()) {
            putString(refreshTokenKey, refreshToken)
                .putString(accessTokenKey, accessToken)
                .apply()
        }
    }

    val refreshToken: String?
        get() = sharedPreferences.getString(refreshTokenKey, null)

    val accessToken: String?
        get() = sharedPreferences.getString(accessTokenKey, null)
}
