package com.example.myapplication.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.R
import com.example.myapplication.domain.model.ProductImage
import com.example.myapplication.domain.model.ProductSize
import com.example.myapplication.ui.common.ErrorReason
import com.example.myapplication.ui.common.HtmlText
import com.example.myapplication.ui.common.LabelBadges
import com.example.myapplication.ui.common.LoadingView
import com.example.myapplication.ui.common.MessageView
import com.example.myapplication.ui.common.ProductImage
import com.example.myapplication.ui.common.message

@Composable
fun ProductDetailScreen(
    onBack: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProductDetailContent(state = state, onBack = onBack, onRetry = viewModel::retry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailContent(
    state: ProductDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (state as? ProductDetailUiState.Success)?.detail?.product?.title
                            ?: stringResource(R.string.product_details_title),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when (state) {
            ProductDetailUiState.Loading -> LoadingView(contentModifier)
            is ProductDetailUiState.Error -> MessageView(
                message = state.reason.message(),
                // Retrying can't help when the product doesn't exist.
                onRetry = onRetry.takeIf { state.reason != ErrorReason.NOT_FOUND },
                modifier = contentModifier,
            )

            is ProductDetailUiState.Success -> ProductDetailBody(state.detail, contentModifier)
        }
    }
}

@Composable
private fun ProductDetailBody(detail: ProductDetail, modifier: Modifier = Modifier) {
    val product = detail.product
    Column(modifier.verticalScroll(rememberScrollState())) {
        ImageCarousel(images = product.images, title = product.title)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LabelBadges(product.labels)
            Text(
                product.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    detail.price ?: stringResource(R.string.price_unavailable),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                detail.originalPrice?.let {
                    Text(
                        stringResource(R.string.original_price, it),
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                product.discountPercentage?.let {
                    Text(
                        stringResource(R.string.discount_value, it),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                stringResource(if (product.inStock) R.string.in_stock else R.string.out_of_stock),
                style = MaterialTheme.typography.labelLarge,
                color = if (product.inStock) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            AttributeText(R.string.colour_value, product.colour)
            AttributeText(R.string.fit_value, product.fit)
            AttributeText(R.string.type_value, product.type)
            AttributeText(R.string.sku_value, product.sku)

            if (product.sizes.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.sizes), style = MaterialTheme.typography.titleMedium)
                SizeChips(product.sizes)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.description), style = MaterialTheme.typography.titleMedium)
            if (detail.descriptionHtml.isBlank()) {
                Text(
                    stringResource(R.string.no_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HtmlText(detail.descriptionHtml)
            }
        }
    }
}

@Composable
private fun AttributeText(format: Int, value: String?) {
    if (value.isNullOrBlank()) return
    Text(stringResource(format, value), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun ImageCarousel(images: List<ProductImage>, title: String) {
    val imageModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(4f / 5f)
    if (images.isEmpty()) {
        ProductImage(url = null, contentDescription = title, modifier = imageModifier)
        return
    }
    val pagerState = rememberPagerState { images.size }
    Box {
        HorizontalPager(state = pagerState, key = { images[it].url }) { page ->
            ProductImage(
                url = images[page].url,
                contentDescription = images[page].altText ?: title,
                modifier = imageModifier,
            )
        }
        if (images.size > 1) {
            val pageDescription =
                stringResource(R.string.image_page, pagerState.currentPage + 1, images.size)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .semantics { contentDescription = pageDescription },
            ) {
                repeat(images.size) { index ->
                    val selected = index == pagerState.currentPage
                    Box(
                        Modifier
                            .size(if (selected) 10.dp else 8.dp)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SizeChips(sizes: List<ProductSize>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        sizes.forEach { size ->
            val outOfStockDescription = stringResource(R.string.size_out_of_stock, size.label)
            AssistChip(
                onClick = {},
                enabled = size.inStock,
                label = {
                    Text(
                        size.label,
                        textDecoration = if (size.inStock) null else TextDecoration.LineThrough,
                    )
                },
                modifier = if (size.inStock) {
                    Modifier
                } else {
                    Modifier.semantics { contentDescription = outOfStockDescription }
                },
            )
        }
    }
}
