package com.crisiscleanup.feature.organizationmanage.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.LIST_DETAIL_DETAIL_WEIGHT
import com.crisiscleanup.core.designsystem.component.LIST_DETAIL_LIST_WEIGHT
import com.crisiscleanup.core.designsystem.component.ListOptionsDropdown
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.RegisterSuccessView
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.listDetailDetailMaxWidth
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.neutralFontColor
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.OrganizationIdName
import com.crisiscleanup.core.selectincident.RefreshIncidentsView
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.organizationmanage.InviteOrgState
import com.crisiscleanup.feature.organizationmanage.InviteTeammateViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteTeammateRoute(
    onBack: () -> Unit = {},
    viewModel: InviteTeammateViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isInviteSent by viewModel.isInviteSent.collectAsStateWithLifecycle()
    val hasValidTokens by viewModel.hasValidTokens.collectAsStateWithLifecycle()

    val t = LocalAppTranslator.current
    Column {
        TopAppBarBackAction(
            title = t("nav.invite_teammates"),
            onAction = onBack,
        )

        if (isLoading) {
            Box {
                BusyIndicatorFloatingTopCenter(true)
            }
        } else if (isInviteSent) {
            RegisterSuccessView(
                title = viewModel.inviteSentTitle,
                text = viewModel.inviteSentText,
            )
        } else if (hasValidTokens) {
            val isListDetailLayout = LocalDimensions.current.isListDetailWidth

            val inviteToAnotherOrg by viewModel.inviteToAnotherOrg.collectAsStateWithLifecycle()
            val inviteOrgState by viewModel.inviteOrgState.collectAsStateWithLifecycle()

            if (isListDetailLayout) {
                Row {
                    Column(
                        Modifier
                            .weight(LIST_DETAIL_DETAIL_WEIGHT)
                            .sizeIn(maxWidth = listDetailDetailMaxWidth),
                    ) {
                        InviteTeammateContent(
                            inviteToAnotherOrg,
                            inviteOrgState,
                        )
                    }
                    Column(
                        Modifier
                            .weight(LIST_DETAIL_LIST_WEIGHT),
                    ) {
                        QrCodeSection(inviteToAnotherOrg, inviteOrgState)
                    }
                }
            } else {
                InviteTeammateContent(
                    inviteToAnotherOrg,
                    inviteOrgState,
                    showQrCode = true,
                )
            }
        } else {
            Text(
                t("inviteTeammates.sign_in_to_invite"),
                listItemModifier,
                style = LocalFontStyles.current.header2,
            )
        }
    }
}

@Composable
fun InviteTeammateContent(
    inviteToAnotherOrg: Boolean,
    inviteOrgState: InviteOrgState,
    viewModel: InviteTeammateViewModel = hiltViewModel(),
    showQrCode: Boolean = false,
) {
    val t = LocalAppTranslator.current
    val closeKeyboard = rememberCloseKeyboard(viewModel)

    val isEditable by viewModel.isEditable.collectAsStateWithLifecycle()

    val inviteToMyOrgText by viewModel.myOrgInviteOptionText.collectAsStateWithLifecycle()
    val inviteToAnotherOrgText = viewModel.anotherOrgInviteOptionText
    val onChangeInvite = remember(viewModel) {
        { inviteToAnother: Boolean ->
            viewModel.inviteToAnotherOrg.value = inviteToAnother
        }
    }

    val organizationNameQuery by viewModel.organizationNameQuery.collectAsStateWithLifecycle()
    val orgQueryResult by viewModel.organizationsSearchResult.collectAsStateWithLifecycle()
    val matchingOrganizations = orgQueryResult.organizations
    var dismissOrganizationQuery by remember { mutableStateOf("") }
    val isOrganizationVisible by remember(
        matchingOrganizations,
        dismissOrganizationQuery,
        organizationNameQuery,
    ) {
        derivedStateOf {
            organizationNameQuery.trim() == orgQueryResult.q &&
                matchingOrganizations.isNotEmpty() &&
                dismissOrganizationQuery != organizationNameQuery
        }
    }
    val onOrgSelect = remember(viewModel) {
        { organization: OrganizationIdName ->
            dismissOrganizationQuery = organization.name
            viewModel.onSelectOrganization(organization)
        }
    }
    val onDismissOrgOptions = remember(viewModel) {
        {
            dismissOrganizationQuery = organizationNameQuery
        }
    }
    val onQueryFocusOut = remember(viewModel) {
        {
            viewModel.onOrgQueryClose()
        }
    }

    val isNewOrganization = inviteOrgState.new
    val searchOrgStartSpace = 48.dp

    val sendInviteErrorMessage by viewModel.sendInviteErrorMessage.collectAsStateWithLifecycle()
    val isSendingInvite by viewModel.isSendingInvite.collectAsStateWithLifecycle()

    var focusOnOrgName by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    var contentSize by remember { mutableStateOf(Size.Zero) }
    Column(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .fillMaxSize()
            .verticalScroll(scrollState)
            .onGloballyPositioned {
                contentSize = it.size.toSize()
            },
    ) {
        Text(
            t("inviteTeammates.invite_new_user_via_email"),
            listItemModifier,
            style = LocalFontStyles.current.header4,
        )

        val radioModifier = Modifier
            .fillMaxWidth()
            .listItemHeight()
            .listItemPadding()
        CrisisCleanupRadioButton(
            radioModifier,
            !inviteToAnotherOrg,
            text = inviteToMyOrgText,
            onSelect = {
                onChangeInvite(false)
                focusOnOrgName = false
            },
            enabled = isEditable,
        )
        CrisisCleanupRadioButton(
            radioModifier,
            inviteToAnotherOrg,
            text = inviteToAnotherOrgText,
            onSelect = {
                onChangeInvite(true)

                if (organizationNameQuery.isBlank()) {
                    focusOnOrgName = true
                }
            },
            enabled = isEditable,
        )

        Column(
            Modifier
                .listItemHorizontalPadding()
                .padding(start = searchOrgStartSpace),
        ) {
            OrgQueryInput(
                isOrganizationsVisible = isOrganizationVisible,
                isEditable = isEditable && inviteToAnotherOrg,
                hasFocus = focusOnOrgName,
                organizationNameQuery = organizationNameQuery,
                updateOrgNameQuery = {
                    dismissOrganizationQuery = ""
                    viewModel.organizationNameQuery.value = it
                },
                organizations = matchingOrganizations,
                onOrgSelect = onOrgSelect,
                onDismissDropdown = onDismissOrgOptions,
                onFocusOut = onQueryFocusOut,
            )

            if (inviteToAnotherOrg) {
                var messageKey = ""
                if (isNewOrganization) {
                    // TODO Hide or show loading when orgs are being queried
                    if (!isOrganizationVisible) {
                        messageKey = "inviteTeammates.org_does_not_have_account"
                    }
                } else if (inviteOrgState.nonAffiliate) {
                    // TODO Update once logic is decided
                    // return "inviteTeammates.user_needs_approval_from_org"
                }
                if (messageKey.isNotBlank()) {
                    Text(
                        t(messageKey),
                        Modifier.listItemVerticalPadding(),
                        color = primaryBlueColor,
                    )
                }
            }
        }

        UserInfoErrorText(
            viewModel.emailAddressError,
            Modifier.padding(top = 16.dp),
        )

        if (inviteOrgState.nonAffiliate) {
            Text(
                t("inviteTeammates.no_unaffiliated_invitations_allowed"),
                listItemModifier,
                color = primaryBlueColor,
            )
        } else {
            val hasEmailError = viewModel.emailAddressError.isNotBlank()
            val emailLabel = t("invitationsVue.email")
            OutlinedClearableTextField(
                modifier = listItemModifier,
                labelResId = 0,
                label = emailLabel,
                value = viewModel.inviteEmailAddresses,
                keyboardType = KeyboardType.Email,
                onValueChange = { viewModel.inviteEmailAddresses = it },
                leadingIcon = {
                    Icon(
                        imageVector = CrisisCleanupIcons.Mail,
                        contentDescription = emailLabel,
                    )
                },
                enabled = isEditable,
                hasFocus = hasEmailError,
                isError = hasEmailError,
            )

            if (isNewOrganization) {
                val coroutineScope = rememberCoroutineScope()
                // TODO Hide or show loading when orgs are being queried
                if (!isOrganizationVisible) {
                    NewOrganizationInput(
                        isEditable,
                        onEndOfInput = {
                            closeKeyboard()
                            coroutineScope.launch {
                                scrollState.animateScrollTo(contentSize.height.toInt())
                            }
                        },
                    )
                }
            } else {
                Text(
                    t("inviteTeammates.use_commas_multiple_emails"),
                    Modifier
                        .listItemHorizontalPadding()
                        // TODO Common dimensions
                        .padding(bottom = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        UserInfoErrorText(sendInviteErrorMessage)

        BusyButton(
            modifier = listItemModifier
                // TODO Common dimensions
                .padding(bottom = 16.dp),
            onClick = viewModel::onSendInvites,
            enabled = !inviteOrgState.nonAffiliate,
            text = t("inviteTeammates.send_invites"),
            indicateBusy = isSendingInvite,
        )

        if (showQrCode) {
            QrCodeSection(inviteToAnotherOrg, inviteOrgState)
        }
    }
}

@Composable
private fun UserInfoErrorText(
    message: String,
    modifier: Modifier = Modifier,
) {
    if (message.isNotBlank()) {
        Text(
            message,
            modifier.then(
                Modifier
                    .listItemHorizontalPadding()
                    .listItemTopPadding(),
            ),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun OrgQueryInput(
    isOrganizationsVisible: Boolean,
    isEditable: Boolean,
    hasFocus: Boolean,
    organizationNameQuery: String,
    updateOrgNameQuery: (String) -> Unit,
    organizations: List<OrganizationIdName>,
    onOrgSelect: (OrganizationIdName) -> Unit,
    onDismissDropdown: () -> Unit,
    onFocusOut: () -> Unit,
) {
    val t = LocalAppTranslator.current

    Box(Modifier.fillMaxWidth()) {
        var contentSize by remember { mutableStateOf(Size.Zero) }

        OutlinedClearableTextField(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    contentSize = it.size.toSize()
                }
                .onFocusChanged {
                    if (!it.isFocused) {
                        onFocusOut()
                    }
                },
            label = t("profileOrg.organization_name"),
            value = organizationNameQuery,
            onValueChange = updateOrgNameQuery,
            keyboardType = KeyboardType.Password,
            keyboardCapitalization = KeyboardCapitalization.Words,
            enabled = isEditable,
            isError = false,
            hasFocus = hasFocus,
        )

        if (organizations.isNotEmpty()) {
            val onSelect = { organization: OrganizationIdName ->
                onOrgSelect(organization)
            }

            val context = LocalContext.current
            val displayMetrics = context.resources.displayMetrics
            val heightPx = displayMetrics.heightPixels
            val maxHeight = remember(heightPx) {
                val density = displayMetrics.density
                val heightDp = heightPx * 0.4 / density
                min(heightDp.dp, 480.dp)
            }

            DropdownMenu(
                modifier = Modifier
                    .sizeIn(maxHeight = maxHeight)
                    .width(
                        with(LocalDensity.current) {
                            contentSize.width
                                .toDp()
                                .minus(listItemDropdownMenuOffset.x.times(2))
                        },
                    ),
                expanded = isOrganizationsVisible,
                onDismissRequest = onDismissDropdown,
                offset = listItemDropdownMenuOffset,
                properties = PopupProperties(focusable = false),
            ) {
                DropdownOrganizationItems(
                    organizations,
                ) {
                    onSelect(it)
                }
            }
        }
    }
}

@Composable
private fun DropdownOrganizationItems(
    organizations: List<OrganizationIdName>,
    onSelect: (OrganizationIdName) -> Unit,
) {
    for (organization in organizations) {
        key(organization.id) {
            DropdownMenuItem(
                modifier = Modifier.optionItemHeight(),
                text = {
                    Text(
                        organization.name,
                        style = LocalFontStyles.current.header4,
                    )
                },
                onClick = { onSelect(organization) },
            )
        }
    }
}

@Composable
private fun NewOrganizationInput(
    isEditable: Boolean,
    onEndOfInput: () -> Unit = {},
    viewModel: InviteTeammateViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current

    val phoneLabel = t("invitationSignup.mobile_placeholder")
    val hasPhoneError = viewModel.phoneNumberError.isNotBlank()
    UserInfoErrorText(viewModel.phoneNumberError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        value = viewModel.invitePhoneNumber,
        onValueChange = { viewModel.invitePhoneNumber = it },
        label = phoneLabel,
        leadingIcon = {
            Icon(
                imageVector = CrisisCleanupIcons.Phone,
                contentDescription = phoneLabel,
            )
        },
        keyboardType = KeyboardType.Password,
        enabled = isEditable,
        hasFocus = hasPhoneError,
        isError = hasPhoneError,
    )

    UserInfoErrorText(viewModel.firstNameError)
    val hasFirstNameError = viewModel.firstNameError.isNotBlank()
    OutlinedClearableTextField(
        modifier = listItemModifier,
        value = viewModel.inviteFirstName,
        onValueChange = { viewModel.inviteFirstName = it },
        label = t("invitationSignup.first_name_placeholder"),
        keyboardType = KeyboardType.Text,
        keyboardCapitalization = KeyboardCapitalization.Words,
        enabled = isEditable,
        hasFocus = hasFirstNameError,
        isError = hasFirstNameError,
    )

    val hasLastNameError = viewModel.lastNameError.isNotBlank()
    UserInfoErrorText(viewModel.lastNameError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        value = viewModel.inviteLastName,
        onValueChange = { viewModel.inviteLastName = it },
        label = t("invitationSignup.last_name_placeholder"),
        keyboardType = KeyboardType.Text,
        keyboardCapitalization = KeyboardCapitalization.Words,
        enabled = isEditable,
        hasFocus = hasLastNameError,
        isError = hasLastNameError,
        imeAction = ImeAction.Done,
        onEnter = onEndOfInput,
    )

    UserInfoErrorText(viewModel.selectedIncidentError)
    val incidentLookup by viewModel.incidentLookup.collectAsStateWithLifecycle()
    val selectedIncident = incidentLookup[viewModel.selectedIncidentId] ?: EmptyIncident
    val selectIncidentHint = t("actions.select_incident")
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()
    if (incidents.isNotEmpty()) {
        Box(
            Modifier
                .listItemBottomPadding()
                .fillMaxWidth(),
        ) {
            var showDropdown by remember { mutableStateOf(false) }
            val onSelect = remember(viewModel) {
                { incident: Incident ->
                    viewModel.selectedIncidentId = incident.id
                    showDropdown = false
                }
            }
            val onHideDropdown = remember(viewModel) { { showDropdown = false } }
            ListOptionsDropdown(
                text = selectedIncident.name.ifBlank { selectIncidentHint },
                isEditable = isEditable,
                onToggleDropdown = { showDropdown = !showDropdown },
                modifier = Modifier.padding(16.dp),
                dropdownIconContentDescription = selectIncidentHint,
            ) { contentSize ->
                IncidentsDropdown(
                    contentSize,
                    showDropdown,
                    onHideDropdown,
                ) {
                    IncidentOptions(
                        incidents,
                        onSelect,
                    )
                }
            }
        }
    } else {
        val isLoadingIncidents by viewModel.isLoadingIncidents.collectAsStateWithLifecycle()
        RefreshIncidentsView(
            isLoadingIncidents,
            viewModel::refreshIncidents,
            listItemModifier,
        )
    }
}

@Composable
private fun IncidentOptions(
    incidents: List<Incident>,
    onSelect: (Incident) -> Unit,
) {
    for (incident in incidents) {
        key(incident.id) {
            DropdownMenuItem(
                text = {
                    Text(
                        incident.name,
                        style = LocalFontStyles.current.header4,
                    )
                },
                onClick = { onSelect(incident) },
                modifier = Modifier.optionItemHeight(),
            )
        }
    }
}

@Composable
private fun QrCodeSection(
    inviteToAnotherOrg: Boolean,
    inviteOrgState: InviteOrgState,
    viewModel: InviteTeammateViewModel = hiltViewModel(),
) {
    val scanQrCodeText by viewModel.scanQrCodeText.collectAsStateWithLifecycle()
    if (inviteOrgState.ownOrAffiliate && scanQrCodeText.isNotBlank()) {
        val t = LocalAppTranslator.current

        val orText = t("inviteTeammates.or")
        Text(
            orText,
            // TODO Common dimensions
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            style = LocalFontStyles.current.header4,
            textAlign = TextAlign.Center,
            color = neutralFontColor,
        )

        Text(
            scanQrCodeText,
            listItemModifier,
            style = LocalFontStyles.current.header4,
        )

        val isGeneratingQrCode by viewModel.isGeneratingQrCode.collectAsStateWithLifecycle()
        if (isGeneratingQrCode) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                AnimatedBusyIndicator(
                    true,
                    padding = 16.dp,
                )
            }
        } else {
            if (inviteToAnotherOrg) {
                val qrCode by viewModel.affiliateOrgQrCode.collectAsStateWithLifecycle()
                qrCode?.let {
                    CenteredRowImage(image = it)
                }
            } else {
                val qrCode by viewModel.myOrgInviteQrCode.collectAsStateWithLifecycle()
                if (qrCode == null) {
                    UserInfoErrorText(t("inviteTeammates.invite_error"))
                } else {
                    CenteredRowImage(image = qrCode!!)
                }
            }
        }
    }
}

@Composable
private fun CenteredRowImage(
    image: ImageBitmap,
) {
    Row(
        Modifier
            .fillMaxWidth()
            // TODO Common dimensions
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(bitmap = image, contentDescription = null)
    }
}
