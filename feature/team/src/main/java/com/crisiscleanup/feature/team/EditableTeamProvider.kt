package com.crisiscleanup.feature.team

import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

interface EditableTeamProvider {
    var incident: Incident
    val editableTeam: MutableStateFlow<CleanupTeam>

    val isStale: Boolean
    fun setStale()
    fun takeStale(): Boolean
}

fun EditableTeamProvider.reset(incidentId: Long = EmptyIncident.id) {
    incident = EmptyIncident
    editableTeam.value = EmptyCleanupTeam.copy(
        incidentId = incidentId,
    )

    takeStale()
}

@Singleton
class SingleEditableTeamProvider @Inject constructor() : EditableTeamProvider {
    override var incident = EmptyIncident
    override val editableTeam = MutableStateFlow(EmptyCleanupTeam)

    private val _isStale = AtomicBoolean(false)
    override val isStale: Boolean
        get() = _isStale.get()

    override fun setStale() {
        _isStale.set(true)
    }

    override fun takeStale() = _isStale.getAndSet(false)
}

@Module
@InstallIn(SingletonComponent::class)
interface EditableTeamModule {
    @Binds
    @Singleton
    fun bindsEditableTeamProvider(provider: SingleEditableTeamProvider): EditableTeamProvider
}
