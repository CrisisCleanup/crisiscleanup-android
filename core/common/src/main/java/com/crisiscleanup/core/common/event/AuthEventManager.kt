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
}

@Singleton
class CrisisCleanupAuthEventManager @Inject constructor() : AuthEventManager {
    private val logoutListenerCounter = AtomicInteger()
    private val logoutListeners = ConcurrentHashMap<Int, WeakReference<LogoutListener>>()

    override fun addLogoutListener(listener: LogoutListener): Int {
        val id = logoutListenerCounter.incrementAndGet()
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
}