package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Hits are kept as raw [JsonElement]s so a single malformed product can be skipped
 * without failing the whole response.
 */
@Serializable
data class ProductResponseDto(
    val hits: List<JsonElement>? = null,
)

@Serializable
data class ProductDto(
    val id: Long? = null,
    val sku: String? = null,
    val inStock: Boolean? = null,
    val availableSizes: List<SizeDto?>? = null,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val fit: String? = null,
    val labels: List<String?>? = null,
    val colour: String? = null,
    val price: Double? = null,
    val compareAtPrice: Double? = null,
    val discountPercentage: Double? = null,
    val featuredMedia: MediaDto? = null,
    val media: List<MediaDto?>? = null,
)

@Serializable
data class MediaDto(
    val id: Long? = null,
    val src: String? = null,
    val alt: String? = null,
    val position: Int? = null,
)

@Serializable
data class SizeDto(
    val size: String? = null,
    val inStock: Boolean? = null,
    val inventoryQuantity: Int? = null,
)
