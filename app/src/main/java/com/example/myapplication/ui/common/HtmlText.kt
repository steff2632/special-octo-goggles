package com.example.myapplication.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration

/** Renders (already sanitised) HTML natively, preserving bold/italic/links/line breaks. */
@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val linkColour = MaterialTheme.colorScheme.primary
    val text = remember(html, linkColour) {
        val parsed = AnnotatedString.fromHtml(
            htmlString = html,
            linkStyles = TextLinkStyles(
                style = SpanStyle(color = linkColour, textDecoration = TextDecoration.Underline),
            ),
        )
        parsed.subSequence(0, parsed.text.trimEnd().length)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}
