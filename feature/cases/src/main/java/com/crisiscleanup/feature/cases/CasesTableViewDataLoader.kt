package com.crisiscleanup.feature.cases

import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.commoncase.TransferWorkTypeProvider
import com.crisiscleanup.core.commoncase.WorkTypeTransferType
import com.crisiscleanup.core.commoncase.WorksiteProvider
import com.crisiscleanup.core.commoncase.model.FormFieldNode
import com.crisiscleanup.core.commoncase.model.WORK_FORM_GROUP_KEY
import com.crisiscleanup.core.commoncase.model.flatten
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.TableWorksiteClaimAction
import com.crisiscleanup.core.model.data.TableWorksiteClaimStatus
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.getClaimStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicReference

class CasesTableViewDataLoader(
    private val worksiteProvider: WorksiteProvider,
    private val worksitesRepository: WorksitesRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val accountDataRepository: AccountDataRepository,
    private val organizationsRepository: OrganizationsRepository,
    private val incidentsRepository: IncidentsRepository,
    private val translator: KeyResourceTranslator,
    private val logger: AppLogger,
) {
    private val isLoadingFlagsWorksite = MutableStateFlow(false)
    private val isLoadingWorkTypeWorksite = MutableStateFlow(false)
    val isLoading = combine(
        isLoadingFlagsWorksite,
        isLoadingWorkTypeWorksite,
    ) { b0, b1 -> b0 || b1 }

    private val worksiteChangingClaimIds = AtomicReference<MutableSet<Long>>(mutableSetOf())
    val worksitesChangingClaimAction = MutableStateFlow<Set<Long>>(emptySet())

    private var incidentWorkTypeLookup = Pair<Long, Map<String, String>>(0, emptyMap())

    suspend fun loadWorksiteForAddFlags(worksite: Worksite): Boolean {
        isLoadingFlagsWorksite.value = true
        try {
            var flagsWorksite = worksite
            if (worksite.id > 0 && worksite.networkId > 0) {
                worksiteChangeRepository.trySyncWorksite(worksite.id)
                flagsWorksite = worksitesRepository.getWorksite(worksite.id)
            }

            worksiteProvider.editableWorksite.value = flagsWorksite

            return true
        } finally {
            isLoadingFlagsWorksite.value = false
        }
    }

    suspend fun onWorkTypeClaimAction(
        worksite: Worksite,
        claimAction: TableWorksiteClaimAction,
        transferWorkTypeProvider: TransferWorkTypeProvider,
    ): WorksiteClaimActionResult {
        synchronized(worksiteChangingClaimIds) {
            val changingIds = worksiteChangingClaimIds.get()
            if (changingIds.contains(worksite.id)) {
                return WorksiteClaimActionResult(isActionInProgress = true)
            }
            changingIds.add(worksite.id)
            worksitesChangingClaimAction.value = changingIds.toSet()
        }

        try {
            var claimStatusWorksite = worksite
            val networkId = worksite.networkId
            if (worksite.id > 0 &&
                networkId > 0 &&
                worksiteChangeRepository.trySyncWorksite(worksite.id)
            ) {
                worksitesRepository.pullWorkTypeRequests(networkId)
                claimStatusWorksite = worksitesRepository.getWorksite(worksite.id)
            }

            return completeTransfer(claimStatusWorksite, transferWorkTypeProvider, claimAction)
        } catch (e: Exception) {
            logger.logException(e)
            return WorksiteClaimActionResult(
                errorMessage = translator("info.error_case_save_mobile")
                    .replace("{case_number}", worksite.caseNumber),
            )
        } finally {
            synchronized(worksiteChangingClaimIds) {
                val changingIds = worksiteChangingClaimIds.get()
                changingIds.remove(worksite.id)
                worksitesChangingClaimAction.value = changingIds.toSet()
            }
        }
    }

    private suspend fun completeTransfer(
        worksite: Worksite,
        transferWorkTypeProvider: TransferWorkTypeProvider,
        claimAction: TableWorksiteClaimAction,
    ): WorksiteClaimActionResult {
        val incidentId = worksite.incidentId

        val myOrg = accountDataRepository.accountData.first().org
        val myOrgId = myOrg.id
        val affiliateIds = organizationsRepository.getOrganizationAffiliateIds(myOrgId, true)

        val claimStatus = worksite.getClaimStatus(affiliateIds)

        val startTransfer = { transferType: WorkTypeTransferType ->
            val requested = worksite.workTypeRequests.filter { it.hasNoResponse }
                .map { it.workType }
            val claimedWorkTypes = worksite.workTypes.filter {
                it.orgClaim != null &&
                    !affiliateIds.contains(it.orgClaim) &&
                    !requested.contains(it.workTypeLiteral)
            }
            worksiteProvider.editableWorksite.value = worksite
            transferWorkTypeProvider.startTransfer(
                myOrgId,
                transferType,
                claimedWorkTypes.associateWith { false },
                myOrg.name,
                worksite.caseNumber,
            )
        }

        when (claimAction) {
            TableWorksiteClaimAction.Claim -> {
                if (claimStatus == TableWorksiteClaimStatus.HasUnclaimed) {
                    val changeWorkTypes = worksite.workTypes.map {
                        if (it.isClaimed) {
                            it
                        } else {
                            it.copy(orgClaim = myOrgId)
                        }
                    }
                    saveChanges(worksite, changeWorkTypes, myOrgId)
                    return WorksiteClaimActionResult(isSuccess = true)
                }
            }

            TableWorksiteClaimAction.Unclaim -> {
                if (claimStatus == TableWorksiteClaimStatus.ClaimedByMyOrg) {
                    val changeWorkTypes = worksite.workTypes.map {
                        if (it.orgClaim != myOrgId) {
                            it
                        } else {
                            it.copy(orgClaim = null)
                        }
                    }
                    saveChanges(worksite, changeWorkTypes, myOrgId)
                    return WorksiteClaimActionResult(isSuccess = true)
                }
            }

            TableWorksiteClaimAction.Request -> {
                if (claimStatus == TableWorksiteClaimStatus.ClaimedByOthers) {
                    setWorkTypeLookup(incidentId)
                    startTransfer(WorkTypeTransferType.Request)
                    return WorksiteClaimActionResult(isSuccess = true)
                }
            }

            TableWorksiteClaimAction.Release -> {
                if (claimStatus == TableWorksiteClaimStatus.ClaimedByOthers) {
                    setWorkTypeLookup(incidentId)
                    startTransfer(WorkTypeTransferType.Release)
                    return WorksiteClaimActionResult(isSuccess = true)
                }
            }
        }

        return WorksiteClaimActionResult(statusChangedTo = claimStatus)
    }

    private suspend fun setWorkTypeLookup(incidentId: Long) {
        if (incidentId != incidentWorkTypeLookup.first) {
            incidentsRepository.getIncident(incidentId, true)?.let { formFieldsIncident ->
                val formFields = FormFieldNode.buildTree(
                    formFieldsIncident.formFields,
                    translator,
                )
                    .map(FormFieldNode::flatten)

                val formFieldTranslationLookup = formFieldsIncident.formFields
                    .filter { it.fieldKey.isNotBlank() && it.label.isNotBlank() }
                    .associate { it.fieldKey to it.label }

                val workTypeFormFields =
                    formFields.firstOrNull { it.fieldKey == WORK_FORM_GROUP_KEY }
                        ?.let { node -> node.children.filter { it.parentKey == WORK_FORM_GROUP_KEY } }
                        ?: emptyList()

                val workTypeTranslationLookup = workTypeFormFields.associate {
                    val name = formFieldTranslationLookup[it.fieldKey] ?: it.fieldKey
                    it.formField.selectToggleWorkType to name
                }

                incidentWorkTypeLookup = Pair(incidentId, workTypeTranslationLookup)
            }
        }
        worksiteProvider.workTypeTranslationLookup = incidentWorkTypeLookup.second
    }

    private suspend fun saveChanges(
        worksite: Worksite,
        changedWorkTypes: List<WorkType>,
        organizationId: Long,
    ) {
        if (worksite.workTypes == changedWorkTypes) {
            return
        }

        val changedWorksite = worksite.copy(workTypes = changedWorkTypes)
        val primaryWorkType = worksite.keyWorkType ?: worksite.workTypes.first()
        worksiteChangeRepository.saveWorksiteChange(
            worksite,
            changedWorksite,
            primaryWorkType,
            organizationId,
        )
    }
}

data class WorksiteClaimActionResult(
    val isSuccess: Boolean = false,
    val isActionInProgress: Boolean = false,
    val statusChangedTo: TableWorksiteClaimStatus? = null,
    val errorMessage: String = "",
)
