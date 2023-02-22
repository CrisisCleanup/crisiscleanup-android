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
}

@Singleton
class CrisisCleanupAuthEventManager @Inject constructor() : AuthEventManager {
    private val listenerCounter = AtomicInteger()
    private val logoutListeners = ConcurrentHashMap<Int, WeakReference<LogoutListener>>()
    private val expiredTokenListeners =
        ConcurrentHashMap<Int, WeakReference<ExpiredTokenListener>>()

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
}