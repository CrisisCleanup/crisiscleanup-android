package com.crisiscleanup.core.designsystem.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

val fillWidthPadded = Modifier
    .fillMaxWidth()
    .padding(16.dp)

val listItemModifier = Modifier
    .fillMaxWidth()
    .listItemPadding()

val listItemHorizontalPadding = PaddingValues(horizontal = 16.dp)
val listItemSpacedBy = Arrangement.spacedBy(16.dp)
val listItemSpacedByHalf = Arrangement.spacedBy(8.dp)

fun Modifier.listItemHeight() = then(heightIn(min = 56.dp))
fun Modifier.listItemPadding() = then(padding(horizontal = 16.dp, vertical = 8.dp))
fun Modifier.listItemHorizontalPadding() = then(padding(horizontal = 16.dp))
fun Modifier.listItemVerticalPadding() = then(padding(vertical = 8.dp))
fun Modifier.listItemTopPadding() = then(padding(top = 8.dp))
fun Modifier.listItemBottomPadding() = then(padding(bottom = 8.dp))
fun Modifier.listRowItemStartPadding() = then(padding(start = 16.dp))
fun Modifier.listItemNestedPadding(nestLevel: Int = 1) =
    then(padding(start = 8.dp.times(nestLevel.coerceAtLeast(1))))

// Horizontal list item padding, vertical option padding
fun Modifier.listItemOptionPadding() = then(padding(16.dp))

fun Modifier.listCheckboxAlignStartOffset() = then(offset(x = (-14).dp))
fun Modifier.listCheckboxAlignItemPaddingCounterOffset() = then(offset(x = 14.dp.plus(16.dp)))

fun Modifier.optionItemHeight() = then(heightIn(min = 56.dp))
fun Modifier.optionItemPadding() = then(padding(top = 16.dp, bottom = 16.dp))

fun Modifier.textBoxHeight() = then(heightIn(min = 128.dp, max = 256.dp))

fun Modifier.textMessagePadding() = then(padding(16.dp))

val listItemDropdownMenuOffset = DpOffset(16.dp, 0.dp)

fun Modifier.centerAlignTextFieldLabelOffset() = then(offset(y = 3.dp))
