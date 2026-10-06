package com.example.myapplication.ui.common

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.domain.util.HtmlSanitizer
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies HTML formatting is actually rendered as styled text, not just passed through. */
@RunWith(AndroidJUnit4::class)
class HtmlTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(html: String) {
        composeRule.setContent {
            MyApplicationTheme { HtmlText(HtmlSanitizer.sanitize(html)) }
        }
    }

    private fun SemanticsNodeInteraction.annotatedText(): AnnotatedString =
        fetchSemanticsNode().config[SemanticsProperties.Text].single()

    /** True when [word] is fully covered by a span style matching [predicate]. */
    private fun AnnotatedString.hasStyle(
        word: String,
        predicate: (androidx.compose.ui.text.SpanStyle) -> Boolean,
    ): Boolean {
        val start = text.indexOf(word)
        require(start >= 0) { "'$word' not found in '$text'" }
        val end = start + word.length
        return spanStyles.any { it.start <= start && it.end >= end && predicate(it.item) }
    }

    @Test
    fun boldTag_isRenderedBold() {
        render("<p>Plain <b>Bold</b></p>")
        val text = composeRule.onNodeWithText("Bold", substring = true).annotatedText()

        assertTrue(text.hasStyle("Bold") { it.fontWeight == FontWeight.Bold })
        assertFalse(text.hasStyle("Plain") { it.fontWeight == FontWeight.Bold })
    }

    @Test
    fun strongTag_isRenderedBold() {
        render("<p><strong>RUN WITH IT</strong></p>")
        val text = composeRule.onNodeWithText("RUN WITH IT").annotatedText()

        assertTrue(text.hasStyle("RUN WITH IT") { it.fontWeight == FontWeight.Bold })
    }

    @Test
    fun uppercaseUnderlineTag_isRenderedUnderlined() {
        render("<p>Some <U>Underlined</U> text</p>")
        val text = composeRule.onNodeWithText("Underlined", substring = true).annotatedText()

        assertTrue(text.hasStyle("Underlined") { it.textDecoration == TextDecoration.Underline })
        assertFalse(text.hasStyle("Some") { it.textDecoration == TextDecoration.Underline })
    }

    @Test
    fun italicAndStrikethroughTags_areRendered() {
        render("<p><i>Italic</i> and <s>Struck</s></p>")
        val text = composeRule.onNodeWithText("Italic", substring = true).annotatedText()

        assertTrue(text.hasStyle("Italic") { it.fontStyle == FontStyle.Italic })
        assertTrue(text.hasStyle("Struck") { it.textDecoration == TextDecoration.LineThrough })
    }

    @Test
    fun anchorTag_isRenderedAsLink() {
        render("<p>Visit <a href=\"https://www.gymshark.com\">Gymshark</a></p>")
        val text = composeRule.onNodeWithText("Gymshark", substring = true).annotatedText()

        val link = text.getLinkAnnotations(0, text.length).single()
        assertEquals("https://www.gymshark.com", (link.item as LinkAnnotation.Url).url)
        assertEquals(text.text.indexOf("Gymshark"), link.start)
    }

    @Test
    fun brTags_becomeLineBreaks_andMarkupIsNotShown() {
        render("<meta charset=\"utf-8\"><p><br>- High rise<br>- Body contouring</p>")
        val text = composeRule.onNodeWithText("High rise", substring = true)
            .assertIsDisplayed()
            .annotatedText()
            .text

        assertEquals("- High rise\n- Body contouring", text)
        assertFalse(text.contains("<"))
        assertFalse(text.contains("meta"))
    }

    @Test
    fun htmlEntities_areDecoded() {
        render("<p>5&#39;3&quot; &amp; size M</p>")
        composeRule.onNodeWithText("5'3\" & size M").assertIsDisplayed()
    }
}
