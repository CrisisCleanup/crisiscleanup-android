package com.crisiscleanup.sync.model

data class SyncPlan(
    val pullIncidents: Boolean,
    val pullIncidentIdWorksites: Long?,
) {
    val requiresSync: Boolean
        get() = pullIncidents || pullIncidentIdWorksites != null

    data class Builder(
        private var pullIncidents: Boolean = false,
        private var pullIncidentIdWorksites: Long? = null,
    ) {
        fun setPullIncidents(): Builder {
            pullIncidents = true
            return this
        }

        fun setPullIncidentIdWorksites(incidentId: Long?): Builder {
            pullIncidentIdWorksites = incidentId
            return this
        }

        fun build(): SyncPlan {
            return SyncPlan(
                pullIncidents,
                pullIncidentIdWorksites,
            )
        }
    }
}