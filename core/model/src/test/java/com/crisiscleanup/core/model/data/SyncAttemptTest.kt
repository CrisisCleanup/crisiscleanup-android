package com.crisiscleanup.core.model.data

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class SyncAttemptTest {
    @Test
    fun isRecent() {
        val syncAttempt = SyncAttempt(10, 100, 1)

        assertTrue(syncAttempt.isRecent(10, 9))
        assertTrue(syncAttempt.isRecent(10, 19))
        assertFalse(syncAttempt.isRecent(10, 20))
        assertFalse(syncAttempt.isRecent(10, 21))
    }

    @Test
    fun isBackingOffAttempt0() {
        val syncAttempt = SyncAttempt(10, 100, 0)

        assertFalse(syncAttempt.isBackingOff(0, 99))
        assertFalse(syncAttempt.isBackingOff(0, 100))
        assertFalse(syncAttempt.isBackingOff(0, 101))
    }

    @Test
    fun isBackingOffAttempt1() {
        val syncAttempt = SyncAttempt(10, 100, 1)

        assertTrue(syncAttempt.isBackingOff(5, 99))
        assertTrue(syncAttempt.isBackingOff(5, 101))
        assertFalse(syncAttempt.isBackingOff(5, 105))
    }

    @Test
    fun isBackingOffAttempt6() {
        val syncAttempt = SyncAttempt(10, 100, 6)

        // 2^5=32
        assertTrue(syncAttempt.isBackingOff(2, 99))
        assertTrue(syncAttempt.isBackingOff(2, 101))
        assertTrue(syncAttempt.isBackingOff(2, 163))
        assertFalse(syncAttempt.isBackingOff(2, 164))
    }

    @Test
    fun backoffIntervalMinimum() {
        val syncAttempt = SyncAttempt(10, 100, 4)

        assertTrue(syncAttempt.isBackingOff(-2, 99))
        assertTrue(syncAttempt.isBackingOff(-2, 107))
        assertFalse(syncAttempt.isBackingOff(-2, 108))

        assertTrue(syncAttempt.isBackingOff(0, 99))
        assertTrue(syncAttempt.isBackingOff(0, 107))
        assertFalse(syncAttempt.isBackingOff(0, 108))
    }
}