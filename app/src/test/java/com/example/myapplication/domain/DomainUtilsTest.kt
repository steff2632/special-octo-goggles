package com.example.myapplication.domain

import com.example.myapplication.domain.model.LabelStyle
import com.example.myapplication.domain.model.ProductLabel
import com.example.myapplication.domain.util.HtmlSanitizer
import com.example.myapplication.domain.util.PriceFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceFormatterTest {
    @Test
    fun `formats pence as GBP`() {
        assertEquals("£10.00", PriceFormatter.format(1000))
        assertEquals("£0.65", PriceFormatter.format(65))
        assertEquals("£0.00", PriceFormatter.format(0))
        assertEquals("£1,234.56", PriceFormatter.format(123456))
    }

    @Test
    fun `invalid prices are null`() {
        assertNull(PriceFormatter.format(null))
        assertNull(PriceFormatter.format(-1))
    }
}

class ProductLabelTest {
    @Test
    fun `maps known labels`() {
        val expected = mapOf(
            "new" to ("New" to LabelStyle.NEW),
            "going-fast" to ("Going Fast" to LabelStyle.URGENT),
            "limited-edition" to ("Limited Edition" to LabelStyle.EXCLUSIVE),
            "popular" to ("Popular" to LabelStyle.POPULAR),
            "recycled-nylon" to ("Recycled Nylon" to LabelStyle.SUSTAINABLE),
            "recycled-polyester" to ("Recycled Polyester" to LabelStyle.SUSTAINABLE),
        )
        expected.forEach { (raw, display) ->
            val label = ProductLabel.fromRaw(raw)!!
            assertEquals(display.first, label.displayName)
            assertEquals(display.second, label.style)
        }
    }

    @Test
    fun `normalises case, whitespace and underscores`() {
        assertEquals(LabelStyle.URGENT, ProductLabel.fromRaw("  Going_Fast ")!!.style)
        assertEquals(LabelStyle.EXCLUSIVE, ProductLabel.fromRaw("LIMITED EDITION")!!.style)
    }

    @Test
    fun `unknown labels are humanised with OTHER style`() {
        val label = ProductLabel.fromRaw("back-in-stock")!!
        assertEquals("Back In Stock", label.displayName)
        assertEquals(LabelStyle.OTHER, label.style)
    }

    @Test
    fun `blank labels are ignored`() {
        assertNull(ProductLabel.fromRaw(null))
        assertNull(ProductLabel.fromRaw(""))
        assertNull(ProductLabel.fromRaw("   "))
    }
}

class HtmlSanitizerTest {
    @Test
    fun `removes meta, script, style and comments`() {
        val html = "<meta charset=\"utf-8\"><style>p{}</style><!-- c --><p>Text</p>" +
            "<SCRIPT type=\"x\">alert(1)</SCRIPT>"
        assertEquals("<p>Text</p>", HtmlSanitizer.sanitize(html))
    }

    @Test
    fun `removes empty paragraphs and leading breaks`() {
        val html = "<p data-x=\"1\"> <br data-x=\"1\"></p>\n<p><br>- Item one<br>- Item two</p>"
        assertEquals("<p>- Item one<br>- Item two</p>", HtmlSanitizer.sanitize(html))
    }

    @Test
    fun `keeps inline formatting`() {
        val result = HtmlSanitizer.sanitize("<p><strong>RUN</strong> <a href=\"https://x\">link</a></p>")
        assertTrue(result.contains("<strong>RUN</strong>"))
        assertTrue(result.contains("<a href=\"https://x\">link</a>"))
    }

    @Test
    fun `null or blank is empty`() {
        assertEquals("", HtmlSanitizer.sanitize(null))
        assertEquals("", HtmlSanitizer.sanitize("  \n "))
        assertFalse(HtmlSanitizer.sanitize("<meta charset=\"utf-8\">").contains("meta"))
    }
}
