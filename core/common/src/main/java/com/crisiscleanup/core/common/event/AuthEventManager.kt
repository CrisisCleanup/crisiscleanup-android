package com.crisiscleanup.core.common.event

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

interface AuthEventManager {
    fun addLogoutListener(listener: LogoutListener): Int
    fun removeLogoutListener(listenerId: Int)
    suspend fun onLogout()

    fun addExpiredTokenListener(listener: ExpiredTokenListener): Int
    fun removeExpiredTokenListener(listenerId: Int)
    fun onExpiredToken()

    fun addCredentialsRequestListener(listener: CredentialsRequestListener): Int
    fun removeCredentialsRequestListener(listenerId: Int)
    fun onPasswordRequest()

    fun addSaveCredentialsListener(listener: SaveCredentialsListener): Int
    fun removeSaveCredentialsListener(listenerId: Int)
    fun onSaveCredentials(emailAddress: String, password: String)

    fun addPasswordResultListener(listener: CredentialsResultListener): Int
    fun removePasswordResultListener(listenerId: Int)
    fun onPasswordCredentialsResult(
        emailAddress: String,
        password: String,
        resultCode: PasswordRequestCode,
    )
}

@Singleton
class CrisisCleanupAuthEventManager @Inject constructor() : AuthEventManager {
    private val listenerCounter = AtomicInteger()

    private val logoutListeners = ConcurrentHashMap<Int, WeakReference<LogoutListener>>()

    private val expiredTokenListeners =
        ConcurrentHashMap<Int, WeakReference<ExpiredTokenListener>>()

    private val credentialsRequestListeners =
        ConcurrentHashMap<Int, WeakReference<CredentialsRequestListener>>()
    private val saveCredentialsListeners =
        ConcurrentHashMap<Int, WeakReference<SaveCredentialsListener>>()
    private val credentialsResultListeners =
        ConcurrentHashMap<Int, WeakReference<CredentialsResultListener>>()

    override fun addLogoutListener(listener: LogoutListener): Int {
        val id = listenerCounter.incrementAndGet()
        logoutListeners[id] = WeakReference(listener)
        return id
    }

    override fun removeLogoutListener(listenerId: Int) {
        logoutListeners.remove(listenerId)
    }

    override suspend fun onLogout() {
        // TODO Handle exceptions properly
        logoutListeners.values.onEach { it.get()?.onLogout() }
    }

    override fun addExpiredTokenListener(listener: ExpiredTokenListener): Int {
        val id = listenerCounter.incrementAndGet()
        expiredTokenListeners[id] = WeakReference(listener)
        return id
    }

    override fun removeExpiredTokenListener(listenerId: Int) {
        expiredTokenListeners.remove(listenerId)
    }

    override fun onExpiredToken() {
        // TODO Handle exceptions properly
        expiredTokenListeners.values.onEach { it.get()?.onExpiredToken() }
    }

    override fun addCredentialsRequestListener(listener: CredentialsRequestListener): Int {
        val id = listenerCounter.incrementAndGet()
        credentialsRequestListeners[id] = WeakReference(listener)
        return id
    }

    override fun removeCredentialsRequestListener(listenerId: Int) {
        credentialsRequestListeners.remove(listenerId)
    }

    override fun onPasswordRequest() {
        // TODO Handle exceptions properly
        credentialsRequestListeners.values.onEach { it.get()?.onCredentialsRequest() }
    }

    override fun addSaveCredentialsListener(listener: SaveCredentialsListener): Int {
        val id = listenerCounter.incrementAndGet()
        saveCredentialsListeners[id] = WeakReference(listener)
        return id
    }

    override fun removeSaveCredentialsListener(listenerId: Int) {
        saveCredentialsListeners.remove(listenerId)
    }

    override fun onSaveCredentials(emailAddress: String, password: String) {
        // TODO Handle exceptions properly
        saveCredentialsListeners.values.onEach {
            it.get()?.onSaveCredentials(emailAddress, password)
        }
    }

    override fun addPasswordResultListener(listener: CredentialsResultListener): Int {
        val id = listenerCounter.incrementAndGet()
        credentialsResultListeners[id] = WeakReference(listener)
        return id
    }

    override fun removePasswordResultListener(listenerId: Int) {
        credentialsResultListeners.remove(listenerId)
    }

    override fun onPasswordCredentialsResult(
        emailAddress: String,
        password: String,
        resultCode: PasswordRequestCode
    ) {
        // TODO Handle exceptions properly
        credentialsResultListeners.values.onEach {
            it.get()?.onPasswordCredentialsResult(
                emailAddress, password, resultCode
            )
        }
    }
}