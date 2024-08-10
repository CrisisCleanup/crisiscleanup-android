package com.crisiscleanup.feature.menu

import com.crisiscleanup.core.common.TutorialDirector
import com.crisiscleanup.core.common.TutorialStep
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class MenuTutorialDirector @Inject constructor() : TutorialDirector {
    private val stepFLow = MutableStateFlow(TutorialStep.End)
    override val tutorialStep = stepFLow

    override fun startTutorial() {
        tutorialStep.value = TutorialStep.MenuStart
    }

    override fun skipTutorial() {
        tutorialStep.value = TutorialStep.End
    }

    override fun onNextStep() {
        val nextStep = when (tutorialStep.value) {
            TutorialStep.MenuStart,
            TutorialStep.AppNavBar,
            -> TutorialStep.End

            // TODO Other steps

            else -> TutorialStep.End
        }
        tutorialStep.value = nextStep
    }
}