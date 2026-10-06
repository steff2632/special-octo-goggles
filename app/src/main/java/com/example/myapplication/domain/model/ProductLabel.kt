package com.example.myapplication.domain.model

import java.util.Locale

enum class LabelStyle { NEW, URGENT, EXCLUSIVE, POPULAR, SUSTAINABLE, OTHER }

/**
 * A product state indicator derived from the raw API label (e.g. "going-fast").
 * Unknown labels are kept and shown with [LabelStyle.OTHER] so new backend values still surface.
 */
data class ProductLabel(
    val key: String,
    val displayName: String,
    val style: LabelStyle,
) {
    companion object {
        private val known: Map<String, Pair<String, LabelStyle>> = mapOf(
            "new" to ("New" to LabelStyle.NEW),
            "going-fast" to ("Going Fast" to LabelStyle.URGENT),
            "limited-edition" to ("Limited Edition" to LabelStyle.EXCLUSIVE),
            "popular" to ("Popular" to LabelStyle.POPULAR),
            "recycled-nylon" to ("Recycled Nylon" to LabelStyle.SUSTAINABLE),
            "recycled-polyester" to ("Recycled Polyester" to LabelStyle.SUSTAINABLE),
        )

        fun fromRaw(raw: String?): ProductLabel? {
            val key = raw?.trim()?.lowercase(Locale.ROOT)?.replace(Regex("[\\s_]+"), "-")
            if (key.isNullOrEmpty()) return null
            val match = known[key]
            return if (match != null) {
                ProductLabel(key, match.first, match.second)
            } else {
                ProductLabel(key, humanise(key), LabelStyle.OTHER)
            }
        }

        private fun humanise(key: String): String =
            key.split('-')
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.titlecase(Locale.ROOT) }
                }
    }
}
