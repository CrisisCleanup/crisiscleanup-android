package com.crisiscleanup.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import com.crisiscleanup.core.common.TutorialStep
import com.crisiscleanup.core.ui.LayoutSizePosition

@Composable
internal fun TutorialOverlay(
    tutorialStep: TutorialStep,
    onNextStep: () -> Unit,
    navBarSizePosition: LayoutSizePosition,
) {
    // TODO Test recomposing/caching
    // TODO Animate/morph between steps
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                onClick = onNextStep,
            )
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            },
    ) {
        drawRect(
            color = Black.copy(alpha = 0.8f),
            size = size,
        )

        when (tutorialStep) {
            TutorialStep.MenuStart,
            TutorialStep.AppNavBar,
            ->
                menuTutorialAppNav(
                    getNavBarSpotlightSizeOffset(
                        size,
                        navBarSizePosition,
                    ),
                )

            else -> {}
        }
    }
}

private fun DrawScope.menuTutorialAppNav(
    sizeOffset: SizeOffset,
) {
    drawRoundRect(
        color = Color.White,
        topLeft = sizeOffset.topLeft,
        size = sizeOffset.size,
        cornerRadius = CornerRadius(sizeOffset.size.height * 0.5f),
        blendMode = BlendMode.Clear,
    )
}

private data class SizeOffset(
    val size: Size = Size.Zero,
    val topLeft: Offset = Offset.Zero,
)

private fun getNavBarSpotlightSizeOffset(
    viewSize: Size,
    navBarSizePosition: LayoutSizePosition,
): SizeOffset {
    val navBarSize = navBarSizePosition.size
    val isBarScreenWidth = navBarSize.width > viewSize.width * 0.5f
    val isBarScreenHeight = navBarSize.height > viewSize.height * 0.5

    val spotlightWidth = navBarSize.width * 0.85f
    val spotlightHeight = navBarSize.height * (if (isBarScreenHeight) 0.95f else 0.5f)
    val spotlightSize = Size(spotlightWidth, spotlightHeight)
    val horizontalOffset = 0
    val verticalOffset = if (isBarScreenWidth) -64 else 0
    val spotlightTopLeft = Offset(
        navBarSizePosition.position.x + (navBarSize.width - spotlightSize.width) * 0.5f + horizontalOffset,
        navBarSizePosition.position.y + (navBarSize.height - spotlightSize.height) * 0.5f + verticalOffset,
    )
    return SizeOffset(spotlightSize, spotlightTopLeft)
}