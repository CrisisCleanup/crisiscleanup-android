package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.NetworkType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FavoriteChangeTest {
    private val noFavoriteA = testCoreSnapshot(id = 513)
    private val noFavoriteB = testCoreSnapshot(id = 642)
    private val favoriteIdA = testCoreSnapshot(id = 69, favoriteId = 53)
    private val favoriteIdB = testCoreSnapshot(id = 83, favoriteId = 73)
    private val assignedA = testCoreSnapshot(id = 48, isAssignedToOrgMember = true)
    private val assignedB = testCoreSnapshot(id = 72, isAssignedToOrgMember = true)

    private val nullFavoriteWorksite = testNetworkWorksite()
    private val favoriteWorksite =
        testNetworkWorksite(favorite = NetworkType(53, "", createdAtA))

    @Test
    fun noChange() {
        assertNull(nullFavoriteWorksite.getFavoriteChange(noFavoriteA, noFavoriteB))
        assertNull(nullFavoriteWorksite.getFavoriteChange(favoriteIdA, favoriteIdB))
        assertNull(nullFavoriteWorksite.getFavoriteChange(assignedA, noFavoriteB))
        assertNull(nullFavoriteWorksite.getFavoriteChange(assignedA, favoriteIdB))

        assertNull(favoriteWorksite.getFavoriteChange(assignedA, assignedB))
        assertNull(favoriteWorksite.getFavoriteChange(noFavoriteA, assignedB))
        assertNull(favoriteWorksite.getFavoriteChange(favoriteIdB, assignedB))
    }

    @Test
    fun change() {
        assertEquals(false, favoriteWorksite.getFavoriteChange(assignedA, noFavoriteB))
        assertEquals(false, favoriteWorksite.getFavoriteChange(assignedB, favoriteIdA))

        assertEquals(true, nullFavoriteWorksite.getFavoriteChange(noFavoriteA, assignedB))
        assertEquals(true, nullFavoriteWorksite.getFavoriteChange(favoriteIdB, assignedA))
    }
}
