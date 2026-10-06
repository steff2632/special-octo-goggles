package com.example.myapplication.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.BROKEN_IMAGE_URL
import com.example.myapplication.domain.model.ProductLabel
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommonComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun productImage_missingUrl_showsPlaceholder() {
        composeRule.setContent {
            MyApplicationTheme {
                ProductImage(url = null, contentDescription = "Leggings", modifier = Modifier.size(200.dp))
            }
        }
        composeRule.onNodeWithContentDescription("Image unavailable").assertIsDisplayed()
    }

    @Test
    fun productImage_blankUrl_showsPlaceholder() {
        composeRule.setContent {
            MyApplicationTheme {
                ProductImage(url = "  ", contentDescription = "Leggings", modifier = Modifier.size(200.dp))
            }
        }
        composeRule.onNodeWithContentDescription("Image unavailable").assertIsDisplayed()
    }

    @Test
    fun productImage_failedLoad_showsPlaceholder() {
        composeRule.setContent {
            MyApplicationTheme {
                ProductImage(
                    url = BROKEN_IMAGE_URL,
                    contentDescription = "Leggings",
                    modifier = Modifier.size(200.dp),
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Image unavailable")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Image unavailable").assertIsDisplayed()
    }

    @Test
    fun labelBadges_showAllLabelsUppercased() {
        val labels = listOf("new", "going-fast", "limited-edition", "popular", "recycled-nylon", "back-in-stock")
            .mapNotNull(ProductLabel::fromRaw)
        composeRule.setContent { MyApplicationTheme { LabelBadges(labels) } }

        listOf("NEW", "GOING FAST", "LIMITED EDITION", "POPULAR", "RECYCLED NYLON", "BACK IN STOCK")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun messageView_retryButton_invokesCallback() {
        var retries = 0
        composeRule.setContent {
            MyApplicationTheme { MessageView(message = "Oops", onRetry = { retries++ }) }
        }
        composeRule.onNodeWithText("Oops").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun messageView_withoutRetry_hidesButton() {
        composeRule.setContent { MyApplicationTheme { MessageView(message = "Oops") } }
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
    }
}
