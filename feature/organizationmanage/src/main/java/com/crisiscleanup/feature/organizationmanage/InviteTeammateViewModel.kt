package com.crisiscleanup.feature.organizationmanage

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.AppSettingsProvider
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.QrCodeGenerator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Onboarding
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelectManager
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.OrgVolunteerRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class InviteTeammateViewModel @Inject constructor(
    settingsProvider: AppSettingsProvider,
    accountDataRepository: AccountDataRepository,
    organizationsRepository: OrganizationsRepository,
    orgVolunteerRepository: OrgVolunteerRepository,
    inputValidator: InputValidator,
    qrCodeGenerator: QrCodeGenerator,
    incidentSelectManager: IncidentSelectManager,
    translator: KeyResourceTranslator,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    @Logger(Onboarding) private val logger: AppLogger,
) : ViewModel() {

    private val inviteUrl = "${settingsProvider.baseUrl}/mobile_app_user_invite"

}