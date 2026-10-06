package com.example.myapplication.domain.util

/**
 * Cleans CMS-generated HTML so it renders tidily as native text:
 * drops non-content tags (meta/script/style), empty paragraphs and leading line breaks.
 */
object HtmlSanitizer {
    private val blockTags = Regex(
        "<(script|style)[^>]*>.*?</\\1\\s*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val metaTags = Regex("<meta[^>]*>", RegexOption.IGNORE_CASE)
    private val comments = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private const val BR = "<br[^>]*>"
    private val emptyParagraph = Regex(
        "<p[^>]*>(\\s|&nbsp;|\u00A0|$BR)*</p>",
        RegexOption.IGNORE_CASE,
    )
    private val leadingBreaks = Regex("(<p[^>]*>)(\\s|$BR)+", RegexOption.IGNORE_CASE)
    private val newlines = Regex("[\\r\\n]+")

    fun sanitize(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return html
            .replace(blockTags, "")
            .replace(comments, "")
            .replace(metaTags, "")
            .replace(newlines, " ")
            .replace(emptyParagraph, "")
            .replace(leadingBreaks, "$1")
            .trim()
    }
}
