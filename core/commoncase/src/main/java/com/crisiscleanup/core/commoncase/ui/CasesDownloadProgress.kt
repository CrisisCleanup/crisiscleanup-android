package com.crisiscleanup.core.commoncase.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.model.data.DataProgressMetrics

@Composable
fun BoxScope.CasesDownloadProgress(dataProgress: DataProgressMetrics) {
    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth(),
        visible = dataProgress.showProgress,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        var progressColor = primaryOrangeColor
        if (dataProgress.isSecondaryData) {
            progressColor = progressColor.disabledAlpha()
        }
        LinearProgressIndicator(
            progress = { dataProgress.progress },
            color = progressColor,
        )
    }
}
