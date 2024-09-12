package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NetworkOrganizationTest {
    @Test
    fun getIncidentOrganizations() {
        val result =
            TestUtil.decodeResource<NetworkOrganizationsResult>("/incidentOrganizations.json")

        assertNull(result.errors)

        assertEquals(391, result.count)

        val organizations = result.results
        assertNotNull(organizations)
        assertEquals(2, organizations.size)
        val expected = NetworkIncidentOrganization(
            id = 5120,
            name = "test",
            affiliates = listOf(5120),
            isActive = false,
            primaryLocation = 79749,
            secondaryLocation = null,
            typeT = "orgType.government",
            primaryContacts = listOf(
                NetworkPersonContact(
                    id = 29695,
                    firstName = "test",
                    lastName = "test",
                    email = "test@test.com",
                    mobile = "5353151368",
                ),
            ),
            incidents = listOf(694, 255, 254),
        )
        assertEquals(expected, organizations[0])
    }
}
