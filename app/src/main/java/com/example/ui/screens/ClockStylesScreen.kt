package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.model.ClockStyle
import com.example.model.CustomDesignConfig
import com.example.model.PrayerRowSpacing
import com.example.ui.MainViewModel
import com.example.ui.components.ColorPresetRow
import com.example.ui.components.DigitalText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockStylesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val activeStyle by viewModel.clockStyle.collectAsState()
    val customDesign by viewModel.customDesign.collectAsState()
    val isUrdu = viewModel.language.collectAsState().value == "ur"

    // Local mutable state for editing custom design
    var editingConfig by remember(customDesign) { mutableStateOf(customDesign) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isUrdu) "گھڑی ڈیزائنز" else "Clock Styles & Customizer") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_styles")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isUrdu) "ایک ڈیزائن منتخب کریں" else "Select a Digital Clock Style",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isUrdu) "چار مسجد اور جدید تھیمز یا مکمل اپنی مرضی کا ڈیزائن منتخب کریں" else "Choose from 4 mosque-inspired styles or build your own custom design",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Style Selection Cards (4 Styles + Custom)
            ClockStyle.entries.forEach { style ->
                item(key = style.name) {
                    val isSelected = activeStyle == style
                    val previewConfig = if (style == ClockStyle.STYLE_CUSTOM) editingConfig else PreferencesManager.STYLE_TEMPLATES[style] ?: editingConfig

                    StylePreviewCard(
                        style = style,
                        isSelected = isSelected,
                        config = previewConfig,
                        isUrdu = isUrdu,
                        onClick = {
                            viewModel.setClockStyle(style)
                        }
                    )
                }
            }

            // Customizer Section (Visible always or when tweaking custom style)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("customizer_panel")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUrdu) "ڈیزائن حسبِ ضرورت بنائیں" else "Customize Clock Design",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (activeStyle != ClockStyle.STYLE_CUSTOM) {
                                Button(
                                    onClick = {
                                        viewModel.setClockStyle(ClockStyle.STYLE_CUSTOM)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_switch_to_custom")
                                ) {
                                    Text(if (isUrdu) "کسٹم کریں" else "Use Custom", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Color Presets: Background
                        ColorPresetRow(
                            title = if (isUrdu) "پس منظر کا رنگ" else "Background Color",
                            selectedColorHex = editingConfig.backgroundColorHex,
                            palette = listOf(0xFF000000, 0xFF050505, 0xFF03140D, 0xFF0F141C, 0xFF090817, 0xFF18181B),
                            onColorSelected = { editingConfig = editingConfig.copy(backgroundColorHex = it) }
                        )

                        // Color Presets: Clock Numbers
                        ColorPresetRow(
                            title = if (isUrdu) "گھڑی نمبر کا رنگ" else "Clock Number Color",
                            selectedColorHex = editingConfig.clockNumberColorHex,
                            palette = listOf(0xFFFF1E27, 0xFF00E676, 0xFFFFFFFF, 0xFF00F0FF, 0xFFFFD700, 0xFFF97316),
                            onColorSelected = { editingConfig = editingConfig.copy(clockNumberColorHex = it) }
                        )

                        // Color Presets: Prayer Names
                        ColorPresetRow(
                            title = if (isUrdu) "نماز کے نام کا رنگ" else "Prayer Name Color",
                            selectedColorHex = editingConfig.prayerNameColorHex,
                            palette = listOf(0xFFFFD700, 0xFF69F0AE, 0xFF94A3B8, 0xFFFF007A, 0xFF38BDF8, 0xFFFFFFFF),
                            onColorSelected = { editingConfig = editingConfig.copy(prayerNameColorHex = it) }
                        )

                        // Color Presets: Countdown
                        ColorPresetRow(
                            title = if (isUrdu) "کاؤنٹ ڈاؤن کا رنگ" else "Countdown Color",
                            selectedColorHex = editingConfig.countdownColorHex,
                            palette = listOf(0xFFFF3B30, 0xFF00E676, 0xFF38BDF8, 0xFFFFB800, 0xFFA855F7, 0xFF10B981),
                            onColorSelected = { editingConfig = editingConfig.copy(countdownColorHex = it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Font Size Scale Slider
                        Text(
                            text = "${if (isUrdu) "فونٹ سائز" else "Font Size Scale"}: ${(editingConfig.fontSizeScale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = editingConfig.fontSizeScale,
                            onValueChange = { editingConfig = editingConfig.copy(fontSizeScale = it) },
                            valueRange = 0.8f..1.3f,
                            steps = 4,
                            modifier = Modifier.testTag("slider_font_size")
                        )

                        // Clock Size Scale Slider
                        Text(
                            text = "${if (isUrdu) "گھڑی کا سائز" else "Clock Size Scale"}: ${(editingConfig.clockSizeScale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = editingConfig.clockSizeScale,
                            onValueChange = { editingConfig = editingConfig.copy(clockSizeScale = it) },
                            valueRange = 0.8f..1.4f,
                            steps = 5,
                            modifier = Modifier.testTag("slider_clock_size")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Spacing Selector
                        Text(
                            text = if (isUrdu) "قطاروں کے درمیان فاصلہ" else "Prayer Row Spacing",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PrayerRowSpacing.entries.forEach { sp ->
                                FilterChip(
                                    selected = editingConfig.spacing == sp,
                                    onClick = { editingConfig = editingConfig.copy(spacing = sp) },
                                    label = { Text(sp.displayName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isUrdu) "24 گھنٹے فارمیٹ" else "24-Hour Time Format")
                            Switch(
                                checked = editingConfig.use24Hour,
                                onCheckedChange = { editingConfig = editingConfig.copy(use24Hour = it) },
                                modifier = Modifier.testTag("switch_24h")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isUrdu) "سیکنڈز دکھائیں" else "Show Seconds Pulse")
                            Switch(
                                checked = editingConfig.showSeconds,
                                onCheckedChange = { editingConfig = editingConfig.copy(showSeconds = it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isUrdu) "ہجری تاریخ دکھائیں" else "Show Hijri Date")
                            Switch(
                                checked = editingConfig.showHijri,
                                onCheckedChange = { editingConfig = editingConfig.copy(showHijri = it) },
                                modifier = Modifier.testTag("switch_hijri")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isUrdu) "کاؤنٹ ڈاؤن دکھائیں" else "Show Countdown")
                            Switch(
                                checked = editingConfig.showCountdown,
                                onCheckedChange = { editingConfig = editingConfig.copy(showCountdown = it) },
                                modifier = Modifier.testTag("switch_countdown")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Save Design, Reset Design, Restore Default
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateCustomDesign(editingConfig)
                                    viewModel.setClockStyle(ClockStyle.STYLE_CUSTOM)
                                    saveMessage = if (isUrdu) "ڈیزائن محفوظ ہو گیا!" else "Design saved successfully!"
                                },
                                modifier = Modifier.weight(1f).testTag("btn_save_design")
                            ) {
                                Text(if (isUrdu) "محفوظ کریں" else "Save Design")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.resetToActiveStyle()
                                    editingConfig = viewModel.customDesign.value
                                    saveMessage = if (isUrdu) "ری سیٹ ہو گیا" else "Reset to theme default"
                                },
                                modifier = Modifier.testTag("btn_reset_design")
                            ) {
                                Text(if (isUrdu) "ری سیٹ" else "Reset")
                            }

                            TextButton(
                                onClick = {
                                    viewModel.restoreDefaultDesign()
                                    editingConfig = viewModel.customDesign.value
                                    saveMessage = if (isUrdu) "ڈیفالٹ بحال ہو گیا" else "Restored Red Mosque LED"
                                },
                                modifier = Modifier.testTag("btn_restore_default")
                            ) {
                                Text(if (isUrdu) "ڈیفالٹ" else "Default")
                            }
                        }

                        // Confirmation Toast message
                        saveMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StylePreviewCard(
    style: ClockStyle,
    isSelected: Boolean,
    config: CustomDesignConfig,
    isUrdu: Boolean,
    onClick: () -> Unit
) {
    val bgColor = Color(config.backgroundColorHex)
    val clockColor = Color(config.clockNumberColorHex)
    val prayerNameColor = Color(config.prayerNameColorHex)
    val prayerTimeColor = Color(config.prayerTimeColorHex)
    val countdownColor = Color(config.countdownColorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("style_card_${style.name}")
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUrdu) style.urduName else style.displayName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = if (isUrdu) "فعال" else "ACTIVE",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mini Digital Clock Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DigitalText(
                    text = "05:45:20 PM",
                    color = clockColor,
                    fontSize = 18f,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Maghrib ",
                        color = prayerNameColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    DigitalText(
                        text = "-00:24:15",
                        color = countdownColor,
                        fontSize = 13f
                    )
                }
            }
        }
    }
}
