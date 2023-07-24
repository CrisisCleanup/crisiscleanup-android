package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class SectionContentIndexLookup(
    val maxSectionIndex: Int,
    val maxItemIndex: Int,
    val sectionItem: Map<Int, Int>,
    val itemSection: Map<Int, Int>,
    val sectionItemCount: Map<Int, Int>,
)

@Composable
fun FocusSectionSlider(
    editSections: List<String>,
    modifier: Modifier = Modifier,
    snapToNearestIndex: () -> Unit = {},
    pagerState: LazyListState = rememberLazyListState(),
    scrollToSection: (Int) -> Unit = {},
) {

    val pagerScrollConnection = remember(pagerState) {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                snapToNearestIndex()
                return super.onPostFling(consumed, available)
            }
        }
    }
    LazyRow(
        state = pagerState,
        modifier = Modifier.nestedScroll(pagerScrollConnection),
        contentPadding = listItemHorizontalPadding,
        horizontalArrangement = listItemSpacedBy,
    ) {
        items(editSections.size + 1) { index ->
            Box(
                modifier = modifier
                    .clickable { scrollToSection(index) }
                    .listItemHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (index < editSections.size) {
                    val sectionTitle = editSections[index]
                    androidx.compose.material3.Text(
                        "${index + 1}. $sectionTitle",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                } else {
                    val endFillerItemWidth = LocalConfiguration.current.screenWidthDp.dp * 0.8f
                    Spacer(modifier = Modifier.width(endFillerItemWidth))
                }
            }
        }
    }
}

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

@Composable
fun OnSliderScrollRest(
    sectionCount: Int,
    sliderState: LazyListState,
    onScrollRest: (Int) -> Unit,
) {
    LaunchedEffect(sliderState.isScrollInProgress) {
        if (!sliderState.isScrollInProgress) {
            val snapToIndex = if (sliderState.firstVisibleItemIndex >= sectionCount) {
                sectionCount - 1
            } else {
                sliderState.layoutInfo.visibleItemsInfo.firstOrNull()?.let {
                    // TODO Account for (start) padding/spacing
                    if (it.offset < -it.size * 0.5) {
                        it.index + 1
                    } else {
                        it.index
                    }
                } ?: -1
            }

            if (snapToIndex >= 0) {
                val sectionIndex = snapToIndex.coerceAtMost(sectionCount - 1)
                onScrollRest(sectionIndex)
            }
        }
    }
}

@Composable
fun OnContentScrollRest(
    contentListState: LazyListState,
    indexLookups: SectionContentIndexLookup,
    sectionCollapseStates: SnapshotStateList<Boolean>,
    takeScrollToSection: () -> Boolean = { false },
    onScrollRest: (Int) -> Unit,
) {
    LaunchedEffect(contentListState.isScrollInProgress) {
        if (!contentListState.isScrollInProgress && takeScrollToSection()) {
            var actualItemIndex = contentListState.firstVisibleItemIndex
            sectionCollapseStates.forEachIndexed { index, isCollapsed ->
                indexLookups.sectionItem[index]?.let { sectionItemIndex ->
                    if (actualItemIndex < sectionItemIndex) {
                        return@forEachIndexed
                    }

                    if (isCollapsed) {
                        indexLookups.sectionItemCount[index]?.let { sectionItemCount ->
                            actualItemIndex += sectionItemCount
                        }
                    }

                    if (actualItemIndex >= sectionItemIndex) {
                        return@forEachIndexed
                    }
                } ?: return@forEachIndexed
            }
            val sliderIndex = if (actualItemIndex == 0) {
                0
            } else if (actualItemIndex < indexLookups.maxItemIndex) {
                indexLookups.itemSection[actualItemIndex] ?: -1
            } else {
                indexLookups.maxSectionIndex
            }
            if (sliderIndex >= 0 && sliderIndex <= indexLookups.maxSectionIndex) {
                onScrollRest(sliderIndex)
            }
        }
    }
}

@Composable
fun rememberFocusSectionSliderState(
    rememberKey: Any,
    sectionCollapseStates: SnapshotStateList<Boolean>,
    indexLookups: SectionContentIndexLookup,
): FocusSectionSliderState {
    var snapOnEndScroll by remember { mutableStateOf(false) }
    val rememberSnapOnEndScroll = remember(rememberKey) { { snapOnEndScroll = true } }

    val pagerState = rememberLazyListState()
    val contentListState = rememberLazyListState()

    val coroutineScope = rememberCoroutineScope()
    var isSliderScrollToSection by remember { mutableStateOf(false) }
    val sliderScrollToSectionItem = remember(rememberKey) {
        { sectionIndex: Int, itemIndex: Int ->
            if (sectionIndex >= 0 && sectionIndex < sectionCollapseStates.size) {
                coroutineScope.launch {
                    isSliderScrollToSection = true

                    if (sectionCollapseStates[sectionIndex]) {
                        sectionCollapseStates[sectionIndex] = false
                    }

                    var visibleItemIndex = itemIndex
                    for (i in (sectionIndex - 1) downTo 0) {
                        if (sectionCollapseStates[i]) {
                            indexLookups.sectionItemCount[i]?.let { sectionItemCount ->
                                visibleItemIndex -= sectionItemCount
                            }
                        }
                    }

                    pagerState.animateScrollToItem(sectionIndex)
                    contentListState.animateScrollToItem(visibleItemIndex.coerceAtLeast(0))
                }
            }
        }
    }
    val sliderScrollToSection = remember(rememberKey) {
        { index: Int ->
            indexLookups.sectionItem[index]?.let { itemIndex ->
                sliderScrollToSectionItem(index, itemIndex)
            }
            Unit
        }
    }

    val onSliderScrollRest = remember(pagerState) {
        { sectionIndex: Int ->
            if (snapOnEndScroll) {
                snapOnEndScroll = false
                sliderScrollToSection(sectionIndex)
            }
        }
    }

    val takeScrollToSection = remember(contentListState) {
        {
            if (isSliderScrollToSection) {
                isSliderScrollToSection = false
                false
            } else {
                true
            }
        }
    }

    val onContentScrollRest = remember(contentListState) {
        { sliderIndex: Int ->
            if (sliderIndex != pagerState.firstVisibleItemIndex) {
                coroutineScope.launch {
                    pagerState.animateScrollToItem(sliderIndex)
                }
            }
        }
    }

    return remember(rememberKey) {
        FocusSectionSliderState(
            snapOnEndScroll = rememberSnapOnEndScroll,
            pagerState = pagerState,
            contentListState = contentListState,
            coroutineScope = coroutineScope,
            sliderScrollToSectionItem = sliderScrollToSectionItem,
            sliderScrollToSection = sliderScrollToSection,
            onSliderScrollRest = onSliderScrollRest,
            takeScrollToSection = takeScrollToSection,
            onContentScrollRest = onContentScrollRest,
        )
    }
}

@Composable
fun FocusSectionSlider(
    sectionTitles: List<String>,
    state: FocusSectionSliderState,
    indexLookups: SectionContentIndexLookup,
    sectionCollapseStates: SnapshotStateList<Boolean>,
) {
    // TODO Animate elevation when content scrolls below
    FocusSectionSlider(
        sectionTitles,
        Modifier,
        state.snapOnEndScroll,
        state.pagerState,
        state.sliderScrollToSection,
    )

    OnSliderScrollRest(
        sectionTitles.size,
        state.pagerState,
        state.onSliderScrollRest,
    )

    OnContentScrollRest(
        state.contentListState,
        indexLookups,
        sectionCollapseStates,
        state.takeScrollToSection,
        state.onContentScrollRest,
    )
}

@Stable
class FocusSectionSliderState(
    val snapOnEndScroll: () -> Unit,
    val pagerState: LazyListState,
    val contentListState: LazyListState,
    val coroutineScope: CoroutineScope,
    val sliderScrollToSectionItem: (Int, Int) -> Unit,
    val sliderScrollToSection: (Int) -> Unit,
    val onSliderScrollRest: (Int) -> Unit,
    val takeScrollToSection: () -> Boolean,
    val onContentScrollRest: (Int) -> Unit,
)