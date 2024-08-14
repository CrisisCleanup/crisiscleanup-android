package com.crisiscleanup.core.common

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

interface TutorialDirector {
    val tutorialStep: StateFlow<TutorialStep>

    fun startTutorial()

    /**
     * @return TRUE when there is a next step or FALSE when the tutorial is over
     */
    fun onNextStep(): Boolean

    fun skipTutorial()
}

enum class TutorialStep {
    MenuStart,
    InviteTeammates,
    AppNavBar,
    AccountInfo,
    ProvideAppFeedback,
    IncidentSelect,
    End
}

@Qualifier
@Retention(RUNTIME)
annotation class Tutorials(val director: CrisisCleanupTutorialDirectors)

enum class CrisisCleanupTutorialDirectors {
    Menu
}
