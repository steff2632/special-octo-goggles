package com.example.myapplication.ui.detail

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.BROKEN_IMAGE_URL
import com.example.myapplication.domain.model.Product
import com.example.myapplication.domain.model.ProductImage
import com.example.myapplication.domain.util.HtmlSanitizer
import com.example.myapplication.domain.util.PriceFormatter
import com.example.myapplication.ui.common.ErrorReason
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.uiTestProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var backs = 0
    private var retries = 0

    private fun render(state: ProductDetailUiState) {
        composeRule.setContent {
            MyApplicationTheme {
                ProductDetailContent(state = state, onBack = { backs++ }, onRetry = { retries++ })
            }
        }
    }

    private fun renderProduct(product: Product) = render(
        ProductDetailUiState.Success(
            ProductDetail(
                product = product,
                price = PriceFormatter.format(product.priceInPence),
                originalPrice = if (product.isDiscounted) {
                    PriceFormatter.format(product.compareAtPriceInPence)
                } else {
                    null
                },
                descriptionHtml = HtmlSanitizer.sanitize(product.descriptionHtml),
            )
        )
    )

    private fun scrollTo(text: String) =
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text, substring = true))

    @Test
    fun loading_showsProgressIndicator() {
        render(ProductDetailUiState.Loading)
        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
        composeRule.onNodeWithText("Product details").assertIsDisplayed()
    }

    @Test
    fun networkError_offersRetry() {
        render(ProductDetailUiState.Error(ErrorReason.NETWORK))
        composeRule.onNodeWithText("Couldn't connect", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun notFound_hasNoRetry() {
        render(ProductDetailUiState.Error(ErrorReason.NOT_FOUND))
        composeRule.onNodeWithText("This product could not be found.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun backButton_invokesCallback() {
        render(ProductDetailUiState.Loading)
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun success_showsCoreDetails() {
        renderProduct(uiTestProduct(labels = listOf("limited-edition")))

        composeRule.onNodeWithText("LIMITED EDITION").assertIsDisplayed()
        composeRule.onNodeWithText("£10.00").assertIsDisplayed()
        composeRule.onNodeWithText("In stock").assertIsDisplayed()
        composeRule.onNodeWithText("Colour: Navy").assertIsDisplayed()
        composeRule.onNodeWithText("Fit: High-waisted").assertIsDisplayed()
        scrollTo("SKU: SKU1")
        composeRule.onNodeWithText("Type: Womens Leggings").assertIsDisplayed()
        composeRule.onNodeWithText("SKU: SKU1").assertIsDisplayed()
    }

    @Test
    fun discountedProduct_showsWasPriceAndPercentage() {
        renderProduct(uiTestProduct(price = 800, compareAtPrice = 1000, discountPercentage = 20))

        composeRule.onNodeWithText("£8.00").assertIsDisplayed()
        composeRule.onNodeWithText("Was £10.00").assertIsDisplayed()
        composeRule.onNodeWithText("20% off").assertIsDisplayed()
    }

    @Test
    fun outOfStockProduct_showsOutOfStock() {
        renderProduct(uiTestProduct(inStock = false))
        composeRule.onNodeWithText("Out of stock").assertIsDisplayed()
    }

    @Test
    fun sizes_inStockEnabled_outOfStockDisabled() {
        renderProduct(uiTestProduct())
        scrollTo("Sizes")

        composeRule.onNodeWithText("XS").assertIsEnabled()
        composeRule.onNodeWithContentDescription("M, out of stock").assertIsNotEnabled()
    }

    @Test
    fun noSizes_hidesSizesSection() {
        renderProduct(uiTestProduct(sizes = emptyList()))
        composeRule.onNodeWithText("Sizes").assertDoesNotExist()
    }

    @Test
    fun htmlDescription_isRenderedWithFormatting() {
        renderProduct(uiTestProduct(description = "<meta charset=\"utf-8\"><p><b>RUN WITH IT</b></p>"))
        scrollTo("RUN WITH IT")

        val node = composeRule.onNodeWithText("RUN WITH IT").assertIsDisplayed()
        val text = node.fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertTrue(text.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        composeRule.onNodeWithText("meta", substring = true).assertDoesNotExist()
    }

    @Test
    fun emptyDescription_showsFallback() {
        renderProduct(uiTestProduct(description = "<meta charset=\"utf-8\">"))
        scrollTo("No description available.")
        composeRule.onNodeWithText("No description available.").assertIsDisplayed()
    }

    @Test
    fun noImages_showsPlaceholder() {
        renderProduct(uiTestProduct(images = emptyList()))
        composeRule.onNodeWithContentDescription("Image unavailable").assertIsDisplayed()
    }

    @Test
    fun imageCarousel_swipesBetweenImages() {
        val images = (1..3).map { ProductImage("$BROKEN_IMAGE_URL?i=$it", "Image $it") }
        renderProduct(uiTestProduct(images = images))

        composeRule.onNodeWithContentDescription("Image 1 of 3").assertIsDisplayed()
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Image 2 of 3").assertIsDisplayed()
    }

    @Test
    fun singleImage_hidesPagerIndicator() {
        renderProduct(uiTestProduct(images = listOf(ProductImage(BROKEN_IMAGE_URL, null))))
        composeRule.onNodeWithContentDescription("Image 1 of 1").assertDoesNotExist()
    }
}
