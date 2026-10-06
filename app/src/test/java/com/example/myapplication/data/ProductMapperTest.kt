package com.example.myapplication.data

import com.example.myapplication.data.remote.dto.MediaDto
import com.example.myapplication.data.remote.dto.ProductDto
import com.example.myapplication.data.remote.dto.SizeDto
import com.example.myapplication.domain.model.LabelStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMapperTest {

    private val mapper = ProductMapper()

    private val fullDto = ProductDto(
        id = 1,
        sku = " B3A3E ",
        inStock = true,
        availableSizes = listOf(
            SizeDto(size = "xs", inStock = true),
            SizeDto(size = "s", inStock = false),
        ),
        title = " Speed Leggings ",
        description = "<p>Hi</p>",
        type = "Womens Leggings",
        fit = "",
        labels = listOf("going-fast", "new"),
        colour = "Navy",
        price = 1000.0,
        compareAtPrice = 1500.0,
        discountPercentage = 33.4,
        featuredMedia = MediaDto(src = "https://cdn.example.com/a.jpg", alt = "Front"),
        media = listOf(
            MediaDto(src = "https://cdn.example.com/c.jpg", position = 3),
            MediaDto(src = "https://cdn.example.com/a.jpg", position = 1),
            MediaDto(src = "https://cdn.example.com/b.jpg", position = 2),
        ),
    )

    @Test
    fun `maps a complete product`() {
        val product = mapper.map(fullDto)!!

        assertEquals(1L, product.id)
        assertEquals("Speed Leggings", product.title)
        assertEquals("B3A3E", product.sku)
        assertEquals("Navy", product.colour)
        assertNull("blank fit becomes null", product.fit)
        assertEquals(1000L, product.priceInPence)
        assertEquals(1500L, product.compareAtPriceInPence)
        assertEquals(33, product.discountPercentage)
        assertTrue(product.isDiscounted)
        assertTrue(product.inStock)
        assertEquals(listOf("going-fast", "new"), product.labels.map { it.key })
        assertEquals(listOf("XS" to true, "S" to false), product.sizes.map { it.label to it.inStock })
        assertEquals("<p>Hi</p>", product.descriptionHtml)
    }

    @Test
    fun `featured image comes first, gallery sorted by position and de-duplicated`() {
        val product = mapper.map(fullDto)!!

        assertEquals(
            listOf(
                "https://cdn.example.com/a.jpg",
                "https://cdn.example.com/b.jpg",
                "https://cdn.example.com/c.jpg",
            ),
            product.images.map { it.url },
        )
        assertEquals("Front", product.primaryImage?.altText)
    }

    @Test
    fun `missing featured image falls back to first valid media image`() {
        val product = mapper.map(
            fullDto.copy(
                featuredMedia = null,
                media = listOf(MediaDto(src = ""), null, MediaDto(src = "https://cdn.example.com/b.jpg")),
            )
        )!!

        assertEquals("https://cdn.example.com/b.jpg", product.primaryImage?.url)
    }

    @Test
    fun `no valid images produces empty image list`() {
        val product = mapper.map(
            fullDto.copy(
                featuredMedia = MediaDto(src = "not a url"),
                media = listOf(MediaDto(src = "ftp://cdn.example.com/a.jpg"), MediaDto(src = null)),
            )
        )!!

        assertTrue(product.images.isEmpty())
        assertNull(product.primaryImage)
    }

    @Test
    fun `null, empty and blank labels produce no indicators`() {
        assertTrue(mapper.map(fullDto.copy(labels = null))!!.labels.isEmpty())
        assertTrue(mapper.map(fullDto.copy(labels = emptyList()))!!.labels.isEmpty())
        assertTrue(mapper.map(fullDto.copy(labels = listOf(null, " ")))!!.labels.isEmpty())
    }

    @Test
    fun `duplicate and unknown labels are handled`() {
        val labels = mapper.map(fullDto.copy(labels = listOf("new", "NEW", "eco-friendly")))!!.labels

        assertEquals(listOf("new", "eco-friendly"), labels.map { it.key })
        assertEquals(LabelStyle.OTHER, labels[1].style)
        assertEquals("Eco Friendly", labels[1].displayName)
    }

    @Test
    fun `products without id or title are dropped`() {
        assertNull(mapper.map(fullDto.copy(id = null)))
        assertNull(mapper.map(fullDto.copy(title = null)))
        assertNull(mapper.map(fullDto.copy(title = "   ")))
    }

    @Test
    fun `invalid prices become null and discount is not shown`() {
        val product = mapper.map(
            fullDto.copy(price = -1.0, compareAtPrice = Double.NaN, discountPercentage = 0.0)
        )!!

        assertNull(product.priceInPence)
        assertNull(product.compareAtPriceInPence)
        assertNull(product.discountPercentage)
        assertFalse(product.isDiscounted)
    }

    @Test
    fun `stock is derived from sizes when inStock is missing`() {
        val sizes = listOf(SizeDto(size = "m", inStock = null, inventoryQuantity = 2))
        assertTrue(mapper.map(fullDto.copy(inStock = null, availableSizes = sizes))!!.inStock)

        val noStock = listOf(SizeDto(size = "m", inStock = null, inventoryQuantity = 0), null)
        val product = mapper.map(fullDto.copy(inStock = null, availableSizes = noStock))!!
        assertFalse(product.inStock)
        assertEquals(1, product.sizes.size)
    }

    @Test
    fun `normaliseImageUrl accepts http(s) and protocol-relative only`() {
        assertEquals("https://a.com/x.jpg", ProductMapper.normaliseImageUrl(" https://a.com/x.jpg "))
        assertEquals("https://a.com/x.jpg", ProductMapper.normaliseImageUrl("//a.com/x.jpg"))
        assertEquals("http://a.com/x%20y.jpg", ProductMapper.normaliseImageUrl("http://a.com/x y.jpg"))
        assertNull(ProductMapper.normaliseImageUrl(null))
        assertNull(ProductMapper.normaliseImageUrl(""))
        assertNull(ProductMapper.normaliseImageUrl("x.jpg"))
        assertNull(ProductMapper.normaliseImageUrl("file:///sdcard/x.jpg"))
        assertNull(ProductMapper.normaliseImageUrl("https://"))
        assertNull(ProductMapper.normaliseImageUrl("http://[bad"))
    }
}
