package com.example.myapplication.ui.list

import androidx.compose.runtime.Immutable
import com.example.myapplication.domain.model.Product
import com.example.myapplication.domain.model.ProductLabel
import com.example.myapplication.domain.util.PriceFormatter
import com.example.myapplication.ui.common.ErrorReason

@Immutable
data class ProductListItem(
    val id: Long,
    val title: String,
    val colour: String?,
    val price: String?,
    val originalPrice: String?,
    val imageUrl: String?,
    val imageDescription: String?,
    val labels: List<ProductLabel>,
    val inStock: Boolean,
)

sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data object Empty : ProductListUiState
    data class Error(val reason: ErrorReason) : ProductListUiState
    data class Success(
        val products: List<ProductListItem>,
        val isRefreshing: Boolean = false,
        val refreshError: ErrorReason? = null,
    ) : ProductListUiState
}

fun Product.toListItem(): ProductListItem = ProductListItem(
    id = id,
    title = title,
    colour = colour,
    price = PriceFormatter.format(priceInPence),
    originalPrice = if (isDiscounted) PriceFormatter.format(compareAtPriceInPence) else null,
    imageUrl = primaryImage?.url,
    imageDescription = primaryImage?.altText ?: title,
    labels = labels,
    inStock = inStock,
)
