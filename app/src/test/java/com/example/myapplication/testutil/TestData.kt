package com.example.myapplication.testutil

import com.example.myapplication.domain.model.Product
import com.example.myapplication.domain.model.ProductImage
import com.example.myapplication.domain.model.ProductLabel
import com.example.myapplication.domain.model.ProductSize

fun readResource(name: String): String =
    requireNotNull(object {}.javaClass.classLoader?.getResource(name)) { "Missing resource $name" }
        .readText()

fun testProduct(
    id: Long = 1L,
    title: String = "Speed Leggings",
    price: Long? = 1000,
    compareAtPrice: Long? = null,
    images: List<ProductImage> = listOf(ProductImage("https://cdn.example.com/a.jpg", null)),
    labels: List<ProductLabel> = listOfNotNull(ProductLabel.fromRaw("new")),
    description: String = "<meta charset=\"utf-8\"><p>Hello</p>",
) = Product(
    id = id,
    sku = "SKU$id",
    title = title,
    type = "Leggings",
    colour = "Navy",
    fit = null,
    priceInPence = price,
    compareAtPriceInPence = compareAtPrice,
    discountPercentage = null,
    inStock = true,
    labels = labels,
    images = images,
    sizes = listOf(ProductSize("XS", true)),
    descriptionHtml = description,
)
