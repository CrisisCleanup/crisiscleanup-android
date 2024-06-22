package com.crisiscleanup.core.designsystem.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
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
val listItemCenterSpacedByHalf = Arrangement.spacedBy(
    8.dp,
    alignment = Alignment.CenterVertically,
)
val listItemSpacedByHalf = Arrangement.spacedBy(8.dp)

fun Modifier.listItemHeight() = this.heightIn(min = 56.dp)
fun Modifier.listItemPadding() = this.padding(horizontal = 16.dp, vertical = 8.dp)
fun Modifier.listItemHorizontalPadding() = this.padding(horizontal = 16.dp)
fun Modifier.listItemVerticalPadding() = this.padding(vertical = 8.dp)
fun Modifier.listItemTopPadding() = this.padding(top = 8.dp)
fun Modifier.listItemBottomPadding() = this.padding(bottom = 8.dp)
fun Modifier.listRowItemStartPadding() = this.padding(start = 16.dp)
fun Modifier.listItemNestedPadding(nestLevel: Int = 1) =
    this.padding(start = 8.dp.times(nestLevel.coerceAtLeast(1)))

// Horizontal list item padding, vertical option padding
fun Modifier.listItemOptionPadding() = this.padding(16.dp)

fun Modifier.listCheckboxAlignStartOffset() = this.offset(x = (-14).dp)
fun Modifier.listCheckboxAlignItemPaddingCounterOffset() = this.offset(x = 14.dp.plus(16.dp))

fun Modifier.optionItemHeight() = this.heightIn(min = 56.dp)
fun Modifier.optionItemPadding() = this.padding(top = 16.dp, bottom = 16.dp)

fun Modifier.textBoxHeight() = this.heightIn(min = 128.dp, max = 256.dp)

fun Modifier.textMessagePadding() = this.padding(16.dp)

val listItemDropdownMenuOffset = DpOffset(16.dp, 0.dp)

fun Modifier.centerAlignTextFieldLabelOffset() = this.offset(y = 3.dp)
