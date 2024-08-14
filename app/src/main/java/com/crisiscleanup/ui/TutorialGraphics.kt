package com.crisiscleanup.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.crisiscleanup.core.common.TutorialStep
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.model.data.TutorialViewId
import com.crisiscleanup.core.ui.LayoutSizePosition

@Composable
internal fun TutorialOverlay(
    tutorialStep: TutorialStep,
    onNextStep: () -> Unit,
    tutorialViewLookup: SnapshotStateMap<TutorialViewId, LayoutSizePosition>,
) {
    val t = LocalAppTranslator.current
    val textMeasurer = rememberTextMeasurer()

    val stepForwardText = t("~~(Press anywhere on screen to continue tutorial)")

    val navBarSizePosition = tutorialViewLookup[TutorialViewId.AppNavBar] ?: LayoutSizePosition()

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

        val navBarSize = navBarSizePosition.size
        val isHorizontalBar = navBarSize.width > size.width * 0.5f
        when (tutorialStep) {
            TutorialStep.MenuStart,
            TutorialStep.InviteTeammates,
            TutorialStep.ProvideAppFeedback,
            -> {
                // TODO This requires a bit of testing to guarantee the view is centered...
                val viewSizePosition =
                    tutorialViewLookup[TutorialViewId.InviteTeammate] ?: LayoutSizePosition()
                drawStepForwardText(
                    textMeasurer,
                    stepForwardText,
                    spotlightStepForwardOffset(isHorizontalBar, viewSizePosition),
                )

//                val viewSizeOffset = getDynamicSpotlightSizeOffset(viewSizePosition)
//                val stepInstruction = t("~~Invite teammates to Crisis Cleanup")
//                menuTutorialDynamicContent(
//                    textMeasurer,
//                    isHorizontalBar,
//                    viewSizeOffset,
//                    stepInstruction,
//                )
            }

            TutorialStep.AppNavBar,
            -> {
                val navBarSizeOffset = getNavBarSpotlightSizeOffset(
                    isHorizontalBar,
                    navBarSizePosition,
                )

                val stepForwardOffset = if (isHorizontalBar) {
                    Offset(
                        navBarSizeOffset.topLeft.x,
                        (size.height - navBarSizeOffset.size.height) * 0.3f,
                    )
                } else {
                    Offset(
                        size.width * 0.3f,
                        size.height * 0.3f,
                    )
                }
                drawStepForwardText(
                    textMeasurer,
                    stepForwardText,
                    stepForwardOffset,
                )

                val stepInstruction = t("~~Use the navigation tabs to start Working")
                menuTutorialAppNav(
                    textMeasurer,
                    isHorizontalBar,
                    navBarSizeOffset,
                    stepInstruction,
                )
            }

            TutorialStep.AccountInfo -> {
                drawStepForwardText(
                    textMeasurer,
                    stepForwardText,
                    spotlightAboveStepForwardOffset(isHorizontalBar),
                )

                val viewSizePosition =
                    tutorialViewLookup[TutorialViewId.AccountToggle] ?: LayoutSizePosition()
                val viewSizeOffset = getAppBarSpotlightSizeOffset(viewSizePosition)
                val stepInstruction = t("~~Open account information")
                menuTutorialAccountToggle(
                    textMeasurer,
                    isHorizontalBar,
                    viewSizeOffset,
                    stepInstruction,
                )
            }

            TutorialStep.IncidentSelect -> {
                drawStepForwardText(
                    textMeasurer,
                    stepForwardText,
                    spotlightAboveStepForwardOffset(isHorizontalBar),
                )

                val viewSizePosition = tutorialViewLookup[TutorialViewId.IncidentSelectDropdown]
                    ?: LayoutSizePosition()
                val viewSizeOffset = getAppBarSpotlightSizeOffset(viewSizePosition, 1.2f)
                val stepInstruction = t("~~Select and change Incidents")
                menuTutorialSelectIncident(
                    textMeasurer,
                    viewSizeOffset,
                    stepInstruction,
                )
            }

            else -> {}
        }
    }
}

private fun DrawScope.spotlightStepForwardOffset(
    isHorizontalBar: Boolean,
    viewSizePosition: LayoutSizePosition,
): Offset {
    val center = viewSizePosition.position.y + viewSizePosition.size.height * 0.5f
    val y = size.height * (if (center > size.height * 0.5f) {
        0.3f
    } else {
        0.7f
    })
    val x = if (isHorizontalBar) 32f else size.width * 0.3f
    return Offset(x, y)
}

private fun DrawScope.spotlightAboveStepForwardOffset(isHorizontalBar: Boolean) =
    if (isHorizontalBar) {
        Offset(
            32f,
            size.height * 0.6f,
        )
    } else {
        Offset(
            size.width * 0.3f,
            size.height * 0.6f,
        )
    }

private fun DrawScope.drawStepForwardText(
    textMeasurer: TextMeasurer,
    text: String,
    offset: Offset,
) {
    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft = offset,
        style = TextStyle(
            fontSize = 16.sp,
            color = Color.White,
        ),
        overflow = TextOverflow.Visible,
    )
}

private data class SizeOffset(
    val size: Size = Size.Zero,
    val topLeft: Offset = Offset.Zero,
)

private fun getNavBarSpotlightSizeOffset(
    isHorizontalBar: Boolean,
    navBarSizePosition: LayoutSizePosition,
): SizeOffset {
    val navBarSize = navBarSizePosition.size
    val spotlightWidth = navBarSize.width * 0.85f
    val spotlightHeight = navBarSize.height * (if (isHorizontalBar) 0.5f else 0.95f)
    val spotlightSize = Size(spotlightWidth, spotlightHeight)
    val verticalOffset = if (isHorizontalBar) -64 else 0
    val navBarPosition = navBarSizePosition.position
    val spotlightTopLeft = Offset(
        navBarPosition.x + (navBarSize.width - spotlightSize.width) * 0.5f,
        navBarPosition.y + (navBarSize.height - spotlightSize.height) * 0.5f + verticalOffset,
    )
    return SizeOffset(spotlightSize, spotlightTopLeft)
}

private fun DrawScope.menuTutorialAppNav(
    textMeasurer: TextMeasurer,
    isHorizontalBar: Boolean,
    sizeOffset: SizeOffset,
    stepInstruction: String,
) {
    val instructionOffset = if (isHorizontalBar) {
        Offset(
            sizeOffset.topLeft.x,
            (size.height - sizeOffset.size.height) * 0.5f,
        )
    } else {
        Offset(
            sizeOffset.size.width + size.width * 0.25f,
            size.height * 0.5f,
        )
    }
    val instructionStyle = TextStyle(
        fontSize = 32.sp,
        color = Color.White,
    )
    drawText(
        textMeasurer = textMeasurer,
        text = stepInstruction,
        topLeft = instructionOffset,
        style = instructionStyle,
        overflow = TextOverflow.Visible,
    )

    val instructionConstraints = Constraints(
        maxWidth = (size.width - sizeOffset.topLeft.x * 2).toInt(),
    )
    val textLayout = textMeasurer.measure(
        stepInstruction,
        instructionStyle,
        overflow = TextOverflow.Visible,
        constraints = instructionConstraints,
    )
    val textSize = textLayout.size
    val lineStart = if (isHorizontalBar) {
        Offset(size.width * 0.5f, instructionOffset.y + textSize.height + 16)
    } else {
        Offset(instructionOffset.x - 32, instructionOffset.y + textSize.height * 0.5f)
    }
    val lineEnd = if (isHorizontalBar) {
        Offset(size.width * 0.33f, sizeOffset.topLeft.y - 32)
    } else {
        Offset(sizeOffset.size.width + 64, size.height * 0.5f)
    }
    drawLine(
        Color.White,
        lineStart,
        lineEnd,
        strokeWidth = 16f,
        cap = StrokeCap.Round,
    )

    drawRoundRect(
        color = Color.White,
        topLeft = sizeOffset.topLeft,
        size = sizeOffset.size,
        cornerRadius = CornerRadius(sizeOffset.size.height * 0.5f),
        blendMode = BlendMode.Clear,
    )
}

private fun getAppBarSpotlightSizeOffset(
    accountToggleSizePosition: LayoutSizePosition,
    heightScale: Float = 1.1f,
): SizeOffset {
    val size = accountToggleSizePosition.size
    val spotlightWidth = size.width * 1.1f
    val spotlightHeight = size.height * heightScale
    val spotlightSize = Size(spotlightWidth, spotlightHeight)
    val position = accountToggleSizePosition.position
    val spotlightTopLeft = Offset(
        position.x - (spotlightWidth - size.width) * 0.5f,
        position.y - (spotlightHeight - size.height) * 0.5f,
    )
    return SizeOffset(spotlightSize, spotlightTopLeft)
}

private fun DrawScope.menuTutorialAccountToggle(
    textMeasurer: TextMeasurer,
    isHorizontalBar: Boolean,
    sizeOffset: SizeOffset,
    stepInstruction: String,
) {
    val instructionOffset = Offset(
        size.width * (if (isHorizontalBar) 0.1f else 0.3f),
        size.height * 0.4f,
    )
    val instructionStyle = TextStyle(
        fontSize = 32.sp,
        color = Color.White,
    )

    drawText(
        textMeasurer = textMeasurer,
        text = stepInstruction,
        topLeft = instructionOffset,
        style = instructionStyle,
        overflow = TextOverflow.Visible,
    )

    val instructionConstraints = Constraints(
        maxWidth = (size.width - instructionOffset.x).toInt(),
    )
    val textLayout = textMeasurer.measure(
        stepInstruction,
        instructionStyle,
        overflow = TextOverflow.Visible,
        constraints = instructionConstraints,
    )
    val textSize = textLayout.size

    val lineStartX =
        if (isHorizontalBar) size.width * 0.5f else instructionOffset.x + textSize.width + 32
    val lineStartY = instructionOffset.y + (if (isHorizontalBar) -16 else 0)
    val lineStart = Offset(lineStartX, lineStartY)
    val lineEnd = Offset(sizeOffset.topLeft.x, sizeOffset.topLeft.y + sizeOffset.size.height)
    drawLine(
        Color.White,
        lineStart,
        lineEnd,
        strokeWidth = 16f,
        cap = StrokeCap.Round,
    )

    drawRoundRect(
        color = Color.White,
        topLeft = sizeOffset.topLeft,
        size = sizeOffset.size,
        cornerRadius = CornerRadius(sizeOffset.size.height * 0.5f),
        blendMode = BlendMode.Clear,
    )
}

private fun DrawScope.menuTutorialSelectIncident(
    textMeasurer: TextMeasurer,
    sizeOffset: SizeOffset,
    stepInstruction: String,
) {
    val instructionOffset = Offset(
        size.width * 0.1f,
        size.height * 0.4f,
    )
    val instructionStyle = TextStyle(
        fontSize = 32.sp,
        color = Color.White,
    )

    drawText(
        textMeasurer = textMeasurer,
        text = stepInstruction,
        topLeft = instructionOffset,
        style = instructionStyle,
        overflow = TextOverflow.Visible,
    )

    val lineStartX = size.width * 0.2f
    val lineStartY = instructionOffset.y - 16
    val lineStart = Offset(lineStartX, lineStartY)
    val lineEnd = Offset(
        sizeOffset.topLeft.x + sizeOffset.size.width * 0.5f,
        sizeOffset.topLeft.y + sizeOffset.size.height + 32f,
    )
    drawLine(
        Color.White,
        lineStart,
        lineEnd,
        strokeWidth = 16f,
        cap = StrokeCap.Round,
    )

    drawRoundRect(
        color = Color.White,
        topLeft = sizeOffset.topLeft,
        size = sizeOffset.size,
        cornerRadius = CornerRadius(sizeOffset.size.height * 0.5f),
        blendMode = BlendMode.Clear,
    )
}
