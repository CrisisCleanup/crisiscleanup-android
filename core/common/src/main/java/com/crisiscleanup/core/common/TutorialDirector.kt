package com.crisiscleanup.core.common

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

interface TutorialDirector {
    val tutorialStep: StateFlow<TutorialStep>

    fun startTutorial()
    fun onNextStep()
    fun skipTutorial()
}

enum class TutorialStep {
    MenuStart,
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
