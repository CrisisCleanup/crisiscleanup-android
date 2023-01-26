package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarSearch(
    modifier: Modifier = Modifier,
    q: () -> String = { "" },
    onQueryChange: (String) -> Unit = {},
    // TODO Use constant for padding
    horizontalPadding: Dp = 8.dp,
    verticalPadding: Dp = 4.dp,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    hasFocus: Boolean = true,
    onSearch: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(windowInsets)
            // clip after padding so we don't show the title over the inset area
            .clipToBounds()
    ) {
        OutlinedClearableTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding,
                )
                .align(Alignment.Center),
            labelResId = 0,
            value = q(),
            onValueChange = onQueryChange,
            enabled = true,
            isError = false,
            hasFocus = hasFocus,
            onSearch = onSearch,
            imeAction = ImeAction.Search,
        )
    }
}

@Preview
@Composable
private fun TopAppBarSearchCasesPreview() {
    TopAppBarSearch(
        q = { "searching" },
    )
}
