package com.example.myapplication.data

import com.example.myapplication.data.remote.dto.MediaDto
import com.example.myapplication.data.remote.dto.ProductDto
import com.example.myapplication.data.remote.dto.SizeDto
import com.example.myapplication.domain.model.Product
import com.example.myapplication.domain.model.ProductImage
import com.example.myapplication.domain.model.ProductLabel
import com.example.myapplication.domain.model.ProductSize
import java.net.URI
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class ProductMapper @Inject constructor() {

    /** Returns null when the product lacks the minimum data needed to display it (id + title). */
    fun map(dto: ProductDto): Product? {
        val id = dto.id ?: return null
        val title = dto.title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val sizes = dto.availableSizes.orEmpty().mapNotNull(::mapSize)

        return Product(
            id = id,
            sku = dto.sku.clean(),
            title = title,
            type = dto.type.clean(),
            colour = dto.colour.clean(),
            fit = dto.fit.clean(),
            priceInPence = dto.price.toPence(),
            compareAtPriceInPence = dto.compareAtPrice.toPence(),
            discountPercentage = dto.discountPercentage
                ?.takeIf { it.isFinite() && it > 0 }
                ?.roundToInt(),
            inStock = dto.inStock ?: sizes.any { it.inStock },
            labels = dto.labels.orEmpty()
                .mapNotNull(ProductLabel::fromRaw)
                .distinctBy { it.key },
            images = mapImages(dto.featuredMedia, dto.media),
            sizes = sizes,
            descriptionHtml = dto.description.orEmpty(),
        )
    }

    private fun mapImages(featured: MediaDto?, media: List<MediaDto?>?): List<ProductImage> {
        val gallery = media.orEmpty()
            .filterNotNull()
            .sortedBy { it.position ?: Int.MAX_VALUE }
        return (listOfNotNull(featured) + gallery)
            .mapNotNull { dto ->
                normaliseImageUrl(dto.src)?.let { ProductImage(it, dto.alt.clean()) }
            }
            .distinctBy { it.url }
    }

    private fun mapSize(dto: SizeDto?): ProductSize? {
        if (dto == null) return null
        val label = dto.size.clean() ?: return null
        val inStock = dto.inStock ?: ((dto.inventoryQuantity ?: 0) > 0)
        return ProductSize(label.uppercase(Locale.ROOT), inStock)
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun Double?.toPence(): Long? =
        this?.takeIf { it.isFinite() && it >= 0 }?.roundToLong()

    companion object {
        /** Accepts absolute http(s) or protocol-relative URLs; anything else is treated as missing. */
        fun normaliseImageUrl(raw: String?): String? {
            val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val candidate = if (trimmed.startsWith("//")) "https:$trimmed" else trimmed
            return try {
                val uri = URI(candidate.replace(" ", "%20"))
                val scheme = uri.scheme?.lowercase(Locale.ROOT)
                if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()) {
                    uri.toString()
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
