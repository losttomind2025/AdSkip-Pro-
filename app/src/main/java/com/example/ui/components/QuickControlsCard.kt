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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.theme.ShieldSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun QuickControlsCard(
    isMasterEnabled: Boolean,
    isAutoMuteEnabled: Boolean,
    isTurboModeEnabled: Boolean,
    protectYoutube: Boolean,
    protectBrowsers: Boolean,
    protectGames: Boolean,
    selectedLanguage: String,
    onToggleMaster: (Boolean) -> Unit,
    onToggleAutoMute: (Boolean) -> Unit,
    onToggleTurboMode: (Boolean) -> Unit,
    onToggleProtectYoutube: (Boolean) -> Unit,
    onToggleProtectBrowsers: (Boolean) -> Unit,
    onToggleProtectGames: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ShieldBorder, RoundedCornerShape(20.dp))
            .testTag("quick_controls_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ShieldSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = StringsRes.get("controls_title", selectedLanguage),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Master switch
            ControlToggleRow(
                title = StringsRes.get("master_switch", selectedLanguage),
                description = StringsRes.get("master_switch_desc", selectedLanguage),
                icon = Icons.Default.Bolt,
                iconColor = EmeraldPrimary,
                checked = isMasterEnabled,
                onCheckedChange = onToggleMaster,
                testTag = "toggle_master_switch"
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 0.5.dp,
                color = ShieldBorder
            )

            // Auto Mute switch
            ControlToggleRow(
                title = StringsRes.get("auto_mute", selectedLanguage),
                description = StringsRes.get("auto_mute_desc", selectedLanguage),
                icon = Icons.Default.VolumeOff,
                iconColor = CyanAccent,
                checked = isAutoMuteEnabled,
                onCheckedChange = onToggleAutoMute,
                testTag = "toggle_auto_mute_switch"
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 0.5.dp,
                color = ShieldBorder
            )

            // Turbo Mode switch
            ControlToggleRow(
                title = StringsRes.get("turbo_mode", selectedLanguage),
                description = StringsRes.get("turbo_mode_desc", selectedLanguage),
                icon = Icons.Default.Speed,
                iconColor = Color(0xFFF59E0B),
                checked = isTurboModeEnabled,
                onCheckedChange = onToggleTurboMode,
                testTag = "toggle_turbo_mode_switch"
            )

            Spacer(modifier = Modifier.height(18.dp))

            // App Categories Header
            Text(
                text = StringsRes.get("app_filters_title", selectedLanguage),
                color = TextSecondary(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // YouTube filter
            MiniAppFilterRow(
                title = StringsRes.get("filter_youtube", selectedLanguage),
                icon = Icons.Default.PlayCircle,
                iconColor = Color(0xFFEF4444),
                checked = protectYoutube,
                onCheckedChange = onToggleProtectYoutube,
                testTag = "toggle_filter_youtube"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Browsers filter
            MiniAppFilterRow(
                title = StringsRes.get("filter_browsers", selectedLanguage),
                icon = Icons.Default.Language,
                iconColor = Color(0xFF3B82F6),
                checked = protectBrowsers,
                onCheckedChange = onToggleProtectBrowsers,
                testTag = "toggle_filter_browsers"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Games filter
            MiniAppFilterRow(
                title = StringsRes.get("filter_games", selectedLanguage),
                icon = Icons.Default.Games,
                iconColor = Color(0xFFA855F7),
                checked = protectGames,
                onCheckedChange = onToggleProtectGames,
                testTag = "toggle_filter_games"
            )
        }
    }
}

@Composable
private fun ControlToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldPrimary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = ShieldSurfaceVariant
            )
        )
    }
}

@Composable
private fun MiniAppFilterRow(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ShieldSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldPrimary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = ShieldSurface
            )
        )
    }
}

@Composable
private fun TextSecondary(): Color = Color(0xFFCBD5E1)
