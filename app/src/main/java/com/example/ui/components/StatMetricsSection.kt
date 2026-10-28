package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StringsRes
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ShieldBorder
import com.example.ui.theme.ShieldSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun StatMetricsSection(
    totalSkipped: Int,
    timeSavedSec: Long,
    todaySkipped: Int,
    selectedLanguage: String,
    modifier: Modifier = Modifier
) {
    val formattedTime = formatTimeSaved(timeSavedSec, selectedLanguage)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stat_metrics_row"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricItemCard(
            title = StringsRes.get("total_skipped", selectedLanguage),
            value = totalSkipped.toString(),
            unit = "",
            icon = Icons.Default.FastForward,
            accentColor = EmeraldPrimary,
            modifier = Modifier.weight(1f)
        )
        MetricItemCard(
            title = StringsRes.get("time_saved", selectedLanguage),
            value = formattedTime.first,
            unit = formattedTime.second,
            icon = Icons.Default.Schedule,
            accentColor = CyanAccent,
            modifier = Modifier.weight(1.1f)
        )
        MetricItemCard(
            title = StringsRes.get("today_skipped", selectedLanguage),
            value = todaySkipped.toString(),
            unit = "",
            icon = Icons.Default.Today,
            accentColor = Color(0xFFA855F7),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricItemCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ShieldSurface)
            .border(1.dp, ShieldBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = " $unit",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 13.sp
            )
        }
    }
}

private fun formatTimeSaved(seconds: Long, lang: String): Pair<String, String> {
    val secUnit = StringsRes.get("sec", lang)
    val minUnit = StringsRes.get("min", lang)
    val hrUnit = StringsRes.get("hrs", lang)

    return when {
        seconds < 60 -> Pair(seconds.toString(), secUnit)
        seconds < 3600 -> {
            val mins = seconds / 60
            Pair(mins.toString(), minUnit)
        }
        else -> {
            val hrs = seconds / 3600
            val remMins = (seconds % 3600) / 60
            Pair("$hrs.$remMins", hrUnit)
        }
    }
}
