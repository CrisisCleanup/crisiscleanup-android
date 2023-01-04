package com.crisiscleanup.feature.cases

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,

    ) : ViewModel() {

}