package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.WorksiteInteractor
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Singleton
class CasesWorksiteInteractor @Inject constructor(
    incidentSelector: IncidentSelector,
    @ApplicationScope externalScope: CoroutineScope,
) : WorksiteInteractor {
    private val selectedCases = mutableMapOf<ExistingWorksiteIdentifier, Instant>()

    private val recentlySelectedDuration = 1.hours

    init {
        incidentSelector.incidentId
            .onEach {
                clearSelection()
            }
            .launchIn(externalScope)
    }

    private fun clearSelection() {
        selectedCases.clear()
    }

    override fun onSelectCase(incidentId: Long, worksiteId: Long) {
        val identifier = ExistingWorksiteIdentifier(incidentId, worksiteId)
        selectedCases[identifier] = Clock.System.now()
    }

    override fun wasCaseSelected(
        incidentId: Long,
        worksiteId: Long,
        reference: Instant,
    ): Boolean {
        val identifier = ExistingWorksiteIdentifier(incidentId, worksiteId)
        selectedCases[identifier]?.let { selectedTime ->
            return reference - selectedTime < recentlySelectedDuration
        }
        return false
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface WorksiteInteractorModule {
    @Singleton
    @Binds
    fun bindsWorksiteInteractor(
        worksiteInteractor: CasesWorksiteInteractor,
    ): WorksiteInteractor
}
