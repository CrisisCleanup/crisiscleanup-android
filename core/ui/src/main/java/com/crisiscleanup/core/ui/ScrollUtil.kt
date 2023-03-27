package com.crisiscleanup.core.ui

import android.view.MotionEvent
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.Velocity

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.touchDownConsumer(
    touchAction: () -> Unit,
): Modifier = pointerInteropFilter(
    onTouchEvent = {
        when (it.action) {
            MotionEvent.ACTION_DOWN -> {
                touchAction()
                false
            }
            else -> true
        }
    }
)

fun Modifier.scrollFlingListener(
    listenAction: () -> Unit,
): Modifier = composed {
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                listenAction()
                return super.onPreFling(available)
            }
        }
    }
    nestedScroll(nestedScrollConnection)
}