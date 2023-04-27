package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

data class SectionContentIndexLookup(
    val maxSectionIndex: Int,
    val maxItemIndex: Int,
    val sectionItem: Map<Int, Int>,
    val itemSection: Map<Int, Int>,
    val sectionItemCount: Map<Int, Int>,
)

@Composable
fun rememberSectionContentIndexLookup(sectionItemLookup: Map<Int, Int>): MutableState<SectionContentIndexLookup> {
    var minItemSectionIndex = Int.MAX_VALUE
    var maxSectionIndex = -1
    var maxItemSectionIndex = -1
    val sectionItemCount = mutableMapOf<Int, Int>()
    val itemSectionLookup = mutableMapOf<Int, Int>().apply {
        sectionItemLookup.forEach { entry ->
            put(entry.value, entry.key)
            minItemSectionIndex = minItemSectionIndex.coerceAtMost(entry.value)
            maxSectionIndex = maxSectionIndex.coerceAtLeast(entry.key)
            maxItemSectionIndex = maxItemSectionIndex.coerceAtLeast(entry.value)
            val previousSectionIndex = entry.key - 1
            sectionItemLookup[previousSectionIndex]?.let { previousItemIndex ->
                sectionItemCount[previousSectionIndex] = entry.value - previousItemIndex - 2
            }
        }
        var section = get(minItemSectionIndex)!!
        var itemIndex = minItemSectionIndex
        while (itemIndex < maxItemSectionIndex) {
            if (containsKey(itemIndex)) {
                section = get(itemIndex)!!
            } else {
                put(itemIndex, section)
            }
            itemIndex++
        }
    }

    return remember {
        mutableStateOf(
            SectionContentIndexLookup(
                maxSectionIndex = maxSectionIndex,
                maxItemIndex = maxItemSectionIndex,
                sectionItem = sectionItemLookup,
                itemSection = itemSectionLookup,
                sectionItemCount = sectionItemCount,
            )
        )
    }
}
