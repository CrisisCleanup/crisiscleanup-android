package com.crisiscleanup.core.common.event

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

interface TrimMemoryListener {
    fun onTrimMemory(level: Int)
}

interface TrimMemoryEventManager {
    fun addListener(listener: TrimMemoryListener): Int
    fun removeListener(listenerId: Int)
    fun onTrimMemory(level: Int)
}

@Singleton
class CrisisCleanupTrimMemoryEventManager @Inject constructor() : TrimMemoryEventManager {
    private val listenerCounter = AtomicInteger()
    private val listeners = ConcurrentHashMap<Int, WeakReference<TrimMemoryListener>>()

    override fun addListener(listener: TrimMemoryListener): Int {
        val id = listenerCounter.incrementAndGet()
        listeners[id] = WeakReference(listener)
        return id
    }

    override fun removeListener(listenerId: Int) {
        listeners.remove(listenerId)
    }

    override fun onTrimMemory(level: Int) {
        // TODO Handle exceptions properly
        listeners.values.onEach { it.get()?.onTrimMemory(level) }
    }
}