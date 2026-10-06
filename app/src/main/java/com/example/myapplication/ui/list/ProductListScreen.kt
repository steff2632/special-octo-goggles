package com.example.myapplication.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.R
import com.example.myapplication.domain.model.ProductLabel
import com.example.myapplication.ui.common.LabelBadges
import com.example.myapplication.ui.common.LoadingView
import com.example.myapplication.ui.common.MessageView
import com.example.myapplication.ui.common.ProductImage
import com.example.myapplication.ui.common.message
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun ProductListScreen(
    onProductClick: (Long) -> Unit,
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProductListContent(
        state = state,
        onProductClick = onProductClick,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onRefreshErrorShown = viewModel::onRefreshErrorShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListContent(
    state: ProductListUiState,
    onProductClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshErrorShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshError = (state as? ProductListUiState.Success)?.refreshError
    val refreshFailedMessage = stringResource(R.string.refresh_failed)
    LaunchedEffect(refreshError) {
        if (refreshError != null) {
            snackbarHostState.showSnackbar(refreshFailedMessage)
            onRefreshErrorShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.products_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when (state) {
            ProductListUiState.Loading -> LoadingView(contentModifier)
            ProductListUiState.Empty -> MessageView(
                message = stringResource(R.string.no_products),
                onRetry = onRetry,
                modifier = contentModifier,
            )

            is ProductListUiState.Error -> MessageView(
                message = state.reason.message(),
                onRetry = onRetry,
                modifier = contentModifier,
            )

            is ProductListUiState.Success -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = contentModifier,
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.products, key = { it.id }) { item ->
                        ProductCard(item = item, onClick = { onProductClick(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(item: ProductListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box {
            ProductImage(
                url = item.imageUrl,
                contentDescription = item.imageDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
            )
            LabelBadges(labels = item.labels, modifier = Modifier.padding(8.dp))
        }
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.colour?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = item.price ?: stringResource(R.string.price_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                item.originalPrice?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!item.inStock) {
                Text(
                    text = stringResource(R.string.out_of_stock),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductListPreview() {
    MyApplicationTheme {
        ProductListContent(
            state = ProductListUiState.Success(
                listOf(
                    ProductListItem(
                        1, "Speed Leggings", "Navy", "£10.00", null, null, null,
                        listOfNotNull(ProductLabel.fromRaw("new"), ProductLabel.fromRaw("going-fast")),
                        true,
                    ),
                    ProductListItem(
                        2, "Flex High Waisted Leggings", "Black", "£8.00", "£10.00", "bad-url",
                        null, emptyList(), false,
                    ),
                )
            ),
            onProductClick = {},
            onRetry = {},
            onRefresh = {},
            onRefreshErrorShown = {},
        )
    }
}
