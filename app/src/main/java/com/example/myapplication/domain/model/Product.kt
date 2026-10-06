package com.example.myapplication.domain.model

data class Product(
    val id: Long,
    val sku: String?,
    val title: String,
    val type: String?,
    val colour: String?,
    val fit: String?,
    val priceInPence: Long?,
    val compareAtPriceInPence: Long?,
    val discountPercentage: Int?,
    val inStock: Boolean,
    val labels: List<ProductLabel>,
    val images: List<ProductImage>,
    val sizes: List<ProductSize>,
    val descriptionHtml: String,
) {
    val primaryImage: ProductImage? get() = images.firstOrNull()

    /** True only when a valid, higher "compare at" price exists. */
    val isDiscounted: Boolean
        get() = priceInPence != null &&
            compareAtPriceInPence != null &&
            compareAtPriceInPence > priceInPence
}

data class ProductImage(
    val url: String,
    val altText: String?,
)

data class ProductSize(
    val label: String,
    val inStock: Boolean,
)
