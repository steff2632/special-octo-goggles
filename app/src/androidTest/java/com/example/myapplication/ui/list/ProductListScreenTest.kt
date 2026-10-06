package com.example.myapplication.ui.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.uiTestProduct
import com.example.myapplication.ui.common.ErrorReason
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var clickedId: Long? = null
    private var retries = 0
    private var refreshes = 0
    private var refreshErrorsShown = 0

    private fun render(state: ProductListUiState) {
        composeRule.setContent {
            MyApplicationTheme {
                ProductListContent(
                    state = state,
                    onProductClick = { clickedId = it },
                    onRetry = { retries++ },
                    onRefresh = { refreshes++ },
                    onRefreshErrorShown = { refreshErrorsShown++ },
                )
            }
        }
    }

    private fun success(vararg items: ProductListItem, refreshError: ErrorReason? = null) =
        ProductListUiState.Success(items.toList(), refreshError = refreshError)

    @Test
    fun loading_showsProgressIndicator() {
        render(ProductListUiState.Loading)
        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun empty_showsMessageAndRetry() {
        render(ProductListUiState.Empty)
        composeRule.onNodeWithText("No products available right now.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun networkError_showsConnectionMessage_andRetryInvokesCallback() {
        render(ProductListUiState.Error(ErrorReason.NETWORK))
        composeRule.onNodeWithText("Couldn't connect", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun dataAndUnknownErrors_showSpecificMessages() {
        render(ProductListUiState.Error(ErrorReason.DATA))
        composeRule.onNodeWithText("We received unexpected data from the server.").assertIsDisplayed()
    }

    @Test
    fun success_showsTitleColourPriceAndLabels() {
        render(success(uiTestProduct(labels = listOf("going-fast", "new")).toListItem()))

        composeRule.onNodeWithText("Products").assertIsDisplayed()
        composeRule.onNodeWithText("Speed Leggings").assertIsDisplayed()
        composeRule.onNodeWithText("Navy").assertIsDisplayed()
        composeRule.onNodeWithText("£10.00").assertIsDisplayed()
        composeRule.onNodeWithText("GOING FAST").assertIsDisplayed()
        composeRule.onNodeWithText("NEW").assertIsDisplayed()
    }

    @Test
    fun productWithoutLabels_showsNoBadges() {
        render(success(uiTestProduct(labels = emptyList()).toListItem()))
        composeRule.onNodeWithText("NEW").assertDoesNotExist()
        composeRule.onNodeWithText("GOING FAST").assertDoesNotExist()
    }

    @Test
    fun discountedProduct_showsOriginalPrice() {
        render(success(uiTestProduct(price = 800, compareAtPrice = 1000).toListItem()))
        composeRule.onNodeWithText("£8.00").assertIsDisplayed()
        composeRule.onNodeWithText("£10.00").assertIsDisplayed()
    }

    @Test
    fun missingPriceAndColour_areHandled() {
        render(success(uiTestProduct(price = null, colour = null).toListItem()))
        composeRule.onNodeWithText("Price unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Navy").assertDoesNotExist()
    }

    @Test
    fun outOfStockProduct_showsIndicator() {
        render(success(uiTestProduct(inStock = false).toListItem()))
        composeRule.onNodeWithText("Out of stock").assertIsDisplayed()
    }

    @Test
    fun productWithoutImage_showsPlaceholder() {
        render(success(uiTestProduct(images = emptyList()).toListItem()))
        composeRule.onAllNodesWithContentDescription("Image unavailable")
            .fetchSemanticsNodes()
            .let { assertEquals(1, it.size) }
    }

    @Test
    fun clickingProduct_passesItsId() {
        render(
            success(
                uiTestProduct(id = 1, title = "First").toListItem(),
                uiTestProduct(id = 42, title = "Second").toListItem(),
            )
        )
        composeRule.onNodeWithText("Second").performClick()
        assertEquals(42L, clickedId)
    }

    @Test
    fun pullDown_triggersRefresh() {
        render(success(uiTestProduct().toListItem()))
        composeRule.onNodeWithText("Speed Leggings").performTouchInput { swipeDown() }
        composeRule.waitUntil(5_000) { refreshes > 0 }
        assertEquals(1, refreshes)
    }

    @Test
    fun refreshError_showsSnackbar_keepsContent_andIsConsumed() {
        render(success(uiTestProduct().toListItem(), refreshError = ErrorReason.NETWORK))

        composeRule.onNodeWithText("Couldn't refresh products.").assertIsDisplayed()
        composeRule.onNodeWithText("Speed Leggings").assertIsDisplayed()
        composeRule.waitUntil(15_000) { refreshErrorsShown == 1 }
    }

    @Test
    fun manyProducts_lastItemReachableByScrolling() {
        val items = (1L..30L).map { uiTestProduct(id = it, title = "Product $it").toListItem() }
        render(ProductListUiState.Success(items))

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Product 30"))
        composeRule.onNodeWithText("Product 30").assertIsDisplayed().performClick()
        assertEquals(30L, clickedId)
    }
}
