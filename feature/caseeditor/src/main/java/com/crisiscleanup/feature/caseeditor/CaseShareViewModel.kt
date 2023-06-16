package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.KeyResourceTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CaseShareViewModel @Inject constructor(

    val translator: KeyResourceTranslator,
) : ViewModel()