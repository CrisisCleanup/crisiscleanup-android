package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.crisiscleanup.core.common.ParsedPhoneNumber
import com.crisiscleanup.core.common.PhoneNumberUtil
import com.crisiscleanup.core.common.openDialer
import com.crisiscleanup.core.common.openMaps
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.neutralIconColor

@Composable
fun WorksiteNameView(name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = listItemSpacedBy,
    ) {
        Icon(
            imageVector = CrisisCleanupIcons.Person,
            contentDescription = LocalAppTranslator.current("formLabels.name"),
            tint = neutralIconColor,
            modifier = Modifier.testTag("worksitePersonIcon"),
        )
        Text(
            name,
            modifier = Modifier.testTag("worksiteName"),
        )
    }
}

@Composable
fun WorksiteAddressView(
    fullAddress: String,
    postView: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = listItemSpacedBy,
    ) {
        Icon(
            imageVector = CrisisCleanupIcons.Location,
            contentDescription = LocalAppTranslator.current("casesVue.full_address"),
            tint = neutralIconColor,
            modifier = Modifier.testTag("worksiteLocationIcon"),
        )
        Text(
            fullAddress,
            Modifier
                .testTag("worksiteAddress")
                .weight(1f),
        )
        postView()
    }
}

@Composable
fun WorksiteCallButton(
    phone1: String,
    phone2: String,
    enable: Boolean,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    onShowPhoneNumbers: (List<ParsedPhoneNumber>) -> Unit,
) {
    val context = LocalContext.current
    val enableCall = enable && (phone1.isNotBlank() || phone2.isNotBlank())
    CrisisCleanupOutlinedButton(
        onClick = {
            val parsedNumbers = PhoneNumberUtil.getPhoneNumbers(listOf(phone1, phone2))
            if (parsedNumbers.size == 1 && parsedNumbers.first().parsedNumbers.size == 1) {
                context.openDialer(parsedNumbers.first().parsedNumbers.first())
            } else {
                onShowPhoneNumbers(parsedNumbers)
            }
        },
        enabled = enableCall,
        contentPadding = contentPadding,
    ) {
        Icon(
            imageVector = CrisisCleanupIcons.Phone,
            contentDescription = LocalAppTranslator.current("nav.phone"),
            modifier = Modifier.testTag("worksiteCallAction"),
        )
    }
}

@Composable
fun WorksiteAddressButton(
    geoQuery: String,
    locationQuery: String,
    isEditable: Boolean,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    val context = LocalContext.current
    CrisisCleanupOutlinedButton(
        onClick = {
            val query = geoQuery.ifBlank { locationQuery }
            context.openMaps(query)
        },
        enabled = isEditable && geoQuery.isNotBlank(),
        contentPadding = contentPadding,
    ) {
        Icon(
            imageVector = CrisisCleanupIcons.Directions,
            contentDescription = LocalAppTranslator.current("caseView.directions"),
            modifier = Modifier.testTag("worksiteDirectionsAction"),
        )
    }
}
