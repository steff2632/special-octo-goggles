package com.example.myapplication

import com.example.myapplication.domain.model.Product
import com.example.myapplication.domain.model.ProductImage
import com.example.myapplication.domain.model.ProductLabel
import com.example.myapplication.domain.model.ProductSize

/** Nothing listens on port 1, so the connection is refused immediately and Coil reports an error. */
const val BROKEN_IMAGE_URL = "http://127.0.0.1:1/broken.jpg"

fun uiTestProduct(
    id: Long = 1L,
    title: String = "Speed Leggings",
    colour: String? = "Navy",
    price: Long? = 1000,
    compareAtPrice: Long? = null,
    discountPercentage: Int? = null,
    inStock: Boolean = true,
    labels: List<String> = emptyList(),
    images: List<ProductImage> = emptyList(),
    sizes: List<ProductSize> = listOf(ProductSize("XS", true), ProductSize("M", false)),
    description: String = "<p><b>RUN WITH IT</b></p>",
) = Product(
    id = id,
    sku = "SKU$id",
    title = title,
    type = "Womens Leggings",
    colour = colour,
    fit = "High-waisted",
    priceInPence = price,
    compareAtPriceInPence = compareAtPrice,
    discountPercentage = discountPercentage,
    inStock = inStock,
    labels = labels.mapNotNull(ProductLabel::fromRaw),
    images = images,
    sizes = sizes,
    descriptionHtml = description,
)
