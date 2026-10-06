package com.example.myapplication.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.model.LabelStyle
import com.example.myapplication.domain.model.ProductLabel
import java.util.Locale

private data class BadgeColours(val container: Color, val content: Color)

private fun LabelStyle.colours(): BadgeColours = when (this) {
    LabelStyle.NEW -> BadgeColours(Color(0xFF111111), Color.White)
    LabelStyle.URGENT -> BadgeColours(Color(0xFFD84315), Color.White)
    LabelStyle.EXCLUSIVE -> BadgeColours(Color(0xFF6A1B9A), Color.White)
    LabelStyle.POPULAR -> BadgeColours(Color(0xFF1565C0), Color.White)
    LabelStyle.SUSTAINABLE -> BadgeColours(Color(0xFF2E7D32), Color.White)
    LabelStyle.OTHER -> BadgeColours(Color(0xFFE0E0E0), Color(0xFF212121))
}

@Composable
fun LabelBadge(label: ProductLabel, modifier: Modifier = Modifier) {
    val colours = label.style.colours()
    Surface(
        color = colours.container,
        contentColor = colours.content,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = label.displayName.uppercase(Locale.getDefault()),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun LabelBadges(labels: List<ProductLabel>, modifier: Modifier = Modifier) {
    if (labels.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEach { LabelBadge(it) }
    }
}
