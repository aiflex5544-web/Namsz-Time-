package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ColorPresetRow(
    title: String,
    selectedColorHex: Long,
    palette: List<Long>,
    onColorSelected: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            palette.forEach { colorHex ->
                val isSelected = selectedColorHex == colorHex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(colorHex))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorHex) }
                )
            }
        }
    }
}

@Composable
fun DigitalText(
    text: String,
    color: Color,
    fontSize: Float,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    letterSpacing: Float = 1.5f
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        fontFamily = FontFamily.Monospace,
        letterSpacing = letterSpacing.sp,
        modifier = modifier
    )
}
