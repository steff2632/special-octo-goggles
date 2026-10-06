package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.di.FakeProductRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import javax.inject.Inject

/**
 * End-to-end through the real Activity, Hilt ViewModels and navigation,
 * with the repository replaced by [FakeProductRepository].
 */
abstract class HiltFlowTestBase {

    private val hiltRule = HiltAndroidRule(this)

    // The fake must be seeded before the Activity (and its ViewModel) starts loading.
    private val seedRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                hiltRule.inject()
                repository.products = seedProducts
                repository.failuresRemaining = initialFailures
                base.evaluate()
            }
        }
    }

    protected val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(seedRule).around(composeRule)

    @Inject
    lateinit var repository: FakeProductRepository

    /** Number of initial list loads that fail as if offline. */
    protected open val initialFailures: Int = 0

    protected val seedProducts = listOf(
        uiTestProduct(id = 1, title = "Speed Leggings", labels = listOf("new")),
        uiTestProduct(
            id = 2,
            title = "Flex High Waisted Leggings",
            colour = "Atlas Blue Marl",
            labels = listOf("going-fast"),
            description = "<p><strong>FLEXPRESS YOURSELF</strong></p>",
        ),
    )

    protected fun waitForText(text: String) =
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
}

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProductAppFlowTest : HiltFlowTestBase() {

    @Test
    fun listShowsProducts_andSelectingOneOpensDetail_andBackReturns() {
        waitForText("Flex High Waisted Leggings")
        composeRule.onNodeWithText("GOING FAST").assertIsDisplayed()
        composeRule.onNodeWithText("NEW").assertIsDisplayed()

        composeRule.onNodeWithText("Flex High Waisted Leggings").performClick()

        waitForText("Colour: Atlas Blue Marl")
        composeRule.onNodeWithText("Colour: Atlas Blue Marl").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("FLEXPRESS YOURSELF"))
        composeRule.onNodeWithText("FLEXPRESS YOURSELF").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForText("Products")
        composeRule.onNodeWithText("Speed Leggings").assertIsDisplayed()
    }

    @Test
    fun systemBack_fromDetail_returnsToList() {
        waitForText("Speed Leggings")
        composeRule.onNodeWithText("Speed Leggings").performClick()
        waitForText("Colour: Navy")

        Espresso.pressBack()

        waitForText("Products")
        composeRule.onNodeWithText("Flex High Waisted Leggings").assertIsDisplayed()
    }
}

/** Starts with no connection: the error screen is shown and Retry recovers once "back online". */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProductAppOfflineFlowTest : HiltFlowTestBase() {
    override val initialFailures: Int = 1

    @Test
    fun offlineOnLaunch_showsError_thenRetryLoadsProducts() {
        waitForText("Retry")
        composeRule.onNodeWithText("Couldn't connect", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("Retry").performClick()

        waitForText("Speed Leggings")
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
        assertEquals(2, repository.listLoads)
    }
}
