package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FavoriteChangeTest {
    private val notFavoriteA = testCoreSnapshot(id = 513)
    private val notFavoriteB = testCoreSnapshot(id = 642)
    private val favoriteIdA = testCoreSnapshot(id = 69, favoriteId = 53)
    private val favoriteIdB = testCoreSnapshot(id = 83, favoriteId = 73)
    private val assignedToMemberA = testCoreSnapshot(id = 48, isAssignedToOrgMember = true)
    private val assignedToMemberB = testCoreSnapshot(id = 72, isAssignedToOrgMember = true)

    private val nullFavoriteWorksite = testNetworkWorksite()
    private val favoriteWorksite =
        testNetworkWorksite(favorite = NetworkWorksiteFull.Favorite(53, "", createdAtA))

    @Test
    fun noChange() {
        assertNull(nullFavoriteWorksite.getFavoriteChange(notFavoriteA, notFavoriteB))
        assertNull(nullFavoriteWorksite.getFavoriteChange(favoriteIdA, favoriteIdB))
        assertNull(nullFavoriteWorksite.getFavoriteChange(assignedToMemberA, assignedToMemberB))
        assertNull(nullFavoriteWorksite.getFavoriteChange(favoriteIdA, assignedToMemberB))
        assertNull(nullFavoriteWorksite.getFavoriteChange(assignedToMemberA, favoriteIdB))

        assertNull(nullFavoriteWorksite.getFavoriteChange(assignedToMemberA, notFavoriteB))
        assertNull(favoriteWorksite.getFavoriteChange(notFavoriteA, favoriteIdB))
        assertNull(favoriteWorksite.getFavoriteChange(notFavoriteA, assignedToMemberB))
    }

    @Test
    fun change() {
        assertEquals(false, favoriteWorksite.getFavoriteChange(favoriteIdA, notFavoriteB))
        assertEquals(false, favoriteWorksite.getFavoriteChange(assignedToMemberA, notFavoriteB))

        assertEquals(true, nullFavoriteWorksite.getFavoriteChange(notFavoriteA, favoriteIdB))
        assertEquals(true, nullFavoriteWorksite.getFavoriteChange(notFavoriteA, assignedToMemberB))
    }
}
