package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.model.data.IncidentOrganization

val IncidentOrganization.contactList: List<String>
    get() = primaryContacts.map {
        with(it) {
            listOf(
                fullName,
                "($name)",
                "$email $mobile",
            ).combineTrimText(" ")
        }
    }
