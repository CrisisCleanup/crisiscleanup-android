package com.crisiscleanup.core.designsystem.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import java.lang.Integer.max

val fillWidthPadded = Modifier
    .fillMaxWidth()
    .padding(16.dp)

val listItemModifier = Modifier
    .fillMaxWidth()
    .listItemPadding()

fun Modifier.listItemHeight() = heightIn(min = 56.dp)
fun Modifier.listItemPadding() = padding(horizontal = 16.dp, vertical = 8.dp)
fun Modifier.listItemHorizontalPadding() = padding(horizontal = 16.dp)
fun Modifier.listItemVerticalPadding() = padding(vertical = 8.dp)
fun Modifier.listItemTopPadding() = padding(top = 8.dp)
fun Modifier.listItemBottomPadding() = padding(bottom = 8.dp)
fun Modifier.listRowItemStartPadding() = padding(start = 16.dp)
fun Modifier.listItemNestedPadding(nestLevel: Int = 1) =
    padding(start = 8.dp.times(max(1, nestLevel)))

// Horizontal list item padding, vertical option padding
fun Modifier.listItemOptionPadding() = padding(16.dp)

fun Modifier.listCheckboxAlignStartOffset() = offset(x = (-14).dp)

fun Modifier.optionItemHeight() = heightIn(min = 56.dp)
fun Modifier.optionItemPadding() = padding(top = 16.dp, bottom = 16.dp)

fun Modifier.textBoxHeight() = heightIn(min = 128.dp, max = 256.dp)

fun Modifier.textMessagePadding() = padding(16.dp)

val listItemDropdownMenuOffset = DpOffset(16.dp, 0.dp)

fun Modifier.centerAlignTextFieldLabelOffset() = offset(y = 3.dp)