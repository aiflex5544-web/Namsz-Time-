package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val calcSettings by viewModel.calculationSettings.collectAsState()
    val adhanSettings by viewModel.adhanSettings.collectAsState()
    val language by viewModel.language.collectAsState()
    val isPreviewPlaying by viewModel.isPreviewPlaying.collectAsState()
    val persistentNotif by viewModel.persistentNotificationEnabled.collectAsState()

    val isUrdu = language == "ur"

    var selectedMethodExpanded by remember { mutableStateOf(false) }
    var selectedSoundExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isUrdu) "ترتیبات" else "Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_settings")) {
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
                // Language Selection
                Text(
                    text = if (isUrdu) "زبان (Language)" else "Language",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = language == "en",
                        onClick = { viewModel.setLanguage("en") },
                        label = { Text("English") },
                        modifier = Modifier.testTag("chip_lang_en")
                    )
                    FilterChip(
                        selected = language == "ur",
                        onClick = { viewModel.setLanguage("ur") },
                        label = { Text("اردو (Urdu)") },
                        modifier = Modifier.testTag("chip_lang_ur")
                    )
                }
            }

            // Adhan Alerts Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isUrdu) "اذان کے الرٹس" else "Adhan Prayer Alerts",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isUrdu) "نماز کے وقت اذان اور نوٹیفکیشن" else "Play Adhan audio and notify at prayer times",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = adhanSettings.globalEnabled,
                                onCheckedChange = { viewModel.setAdhanSettings(adhanSettings.copy(globalEnabled = it)) },
                                modifier = Modifier.testTag("switch_global_adhan")
                            )
                        }

                        if (adhanSettings.globalEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Enable All / Disable All Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUrdu) "انفرادی نمازیں" else "Individual Prayers",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            viewModel.setAdhanSettings(
                                                adhanSettings.copy(
                                                    fajrEnabled = true,
                                                    dhuhrEnabled = true,
                                                    asrEnabled = true,
                                                    maghribEnabled = true,
                                                    ishaEnabled = true
                                                )
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (isUrdu) "سب آن" else "Enable All", fontSize = 12.sp)
                                    }
                                    TextButton(
                                        onClick = {
                                            viewModel.setAdhanSettings(
                                                adhanSettings.copy(
                                                    fajrEnabled = false,
                                                    dhuhrEnabled = false,
                                                    asrEnabled = false,
                                                    maghribEnabled = false,
                                                    ishaEnabled = false
                                                )
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (isUrdu) "سب آف" else "Disable All", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Individual Prayer Switches
                            PrayerToggleRow(
                                name = if (isUrdu) Prayer.FAJR.urduName else Prayer.FAJR.englishName,
                                isEnabled = adhanSettings.fajrEnabled,
                                onToggle = { viewModel.setAdhanSettings(adhanSettings.copy(fajrEnabled = it)) }
                            )
                            PrayerToggleRow(
                                name = if (isUrdu) Prayer.DHUHR.urduName else Prayer.DHUHR.englishName,
                                isEnabled = adhanSettings.dhuhrEnabled,
                                onToggle = { viewModel.setAdhanSettings(adhanSettings.copy(dhuhrEnabled = it)) }
                            )
                            PrayerToggleRow(
                                name = if (isUrdu) Prayer.ASR.urduName else Prayer.ASR.englishName,
                                isEnabled = adhanSettings.asrEnabled,
                                onToggle = { viewModel.setAdhanSettings(adhanSettings.copy(asrEnabled = it)) }
                            )
                            PrayerToggleRow(
                                name = if (isUrdu) Prayer.MAGHRIB.urduName else Prayer.MAGHRIB.englishName,
                                isEnabled = adhanSettings.maghribEnabled,
                                onToggle = { viewModel.setAdhanSettings(adhanSettings.copy(maghribEnabled = it)) }
                            )
                            PrayerToggleRow(
                                name = if (isUrdu) Prayer.ISHA.urduName else Prayer.ISHA.englishName,
                                isEnabled = adhanSettings.ishaEnabled,
                                onToggle = { viewModel.setAdhanSettings(adhanSettings.copy(ishaEnabled = it)) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            // Adhan Sound Selection Dropdown
                            Text(
                                text = if (isUrdu) "اذان کی آواز" else "Adhan Alert Sound",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            ExposedDropdownMenuBox(
                                expanded = selectedSoundExpanded,
                                onExpandedChange = { selectedSoundExpanded = !selectedSoundExpanded }
                            ) {
                                OutlinedTextField(
                                    value = adhanSettings.soundType.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = selectedSoundExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = selectedSoundExpanded,
                                    onDismissRequest = { selectedSoundExpanded = false }
                                ) {
                                    AdhanSoundType.entries.forEach { sound ->
                                        DropdownMenuItem(
                                            text = { Text(sound.displayName) },
                                            onClick = {
                                                viewModel.setAdhanSettings(adhanSettings.copy(soundType = sound))
                                                selectedSoundExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Play / Stop Preview Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.toggleAudioPreview() },
                                    modifier = Modifier.testTag("btn_preview_audio"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPreviewPlaying) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Preview",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPreviewPlaying)
                                            (if (isUrdu) "آواز بند کریں" else "Stop Preview")
                                        else
                                            (if (isUrdu) "آواز سنیں" else "Play Preview")
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (isUrdu) "صرف لرزش" else "Vibrate Only", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = adhanSettings.vibrateOnly,
                                        onCheckedChange = { viewModel.setAdhanSettings(adhanSettings.copy(vibrateOnly = it)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Calculation Method & Asr Jurisprudence
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isUrdu) "فلکیاتی طریقہ کار" else "Calculation Method",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = selectedMethodExpanded,
                            onExpandedChange = { selectedMethodExpanded = !selectedMethodExpanded }
                        ) {
                            OutlinedTextField(
                                value = calcSettings.method.displayName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = selectedMethodExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = selectedMethodExpanded,
                                onDismissRequest = { selectedMethodExpanded = false }
                            ) {
                                CalculationMethod.entries.forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method.displayName) },
                                        onClick = {
                                            viewModel.setCalculationSettings(calcSettings.copy(method = method))
                                            selectedMethodExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Asr Method
                        Text(
                            text = if (isUrdu) "عصر فقہی طریقہ" else "Asr Juristic Method",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AsrJuristic.entries.forEach { juristic ->
                                FilterChip(
                                    selected = calcSettings.asrJuristic == juristic,
                                    onClick = { viewModel.setCalculationSettings(calcSettings.copy(asrJuristic = juristic)) },
                                    label = { Text(juristic.displayName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Hijri Date Adjustment Offset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (isUrdu) "چاند کی تاریخ ایڈجسٹمنٹ" else "Hijri Date Offset"}: ${if (calcSettings.hijriOffsetDays > 0) "+${calcSettings.hijriOffsetDays}" else "${calcSettings.hijriOffsetDays}"} days",
                                fontSize = 13.sp
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        if (calcSettings.hijriOffsetDays > -2) {
                                            viewModel.setCalculationSettings(calcSettings.copy(hijriOffsetDays = calcSettings.hijriOffsetDays - 1))
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                IconButton(
                                    onClick = {
                                        if (calcSettings.hijriOffsetDays < 2) {
                                            viewModel.setCalculationSettings(calcSettings.copy(hijriOffsetDays = calcSettings.hijriOffsetDays + 1))
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                }
            }

            // Background Monitoring & Permissions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = "Battery", tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isUrdu) "پس منظر مانیٹرنگ اور اجازتیں" else "Background Monitoring & Alarms",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isUrdu)
                                "نور پلس اینڈرائیڈ کے نیٹو الارم مینیجر کا استعمال کرتے ہوئے پس منظر میں نماز کے اوقات کی نگرانی کرتا ہے۔ شیڈول برقرار رکھنے کے لیے اگر ضروری ہو تو ایپ کے لیے بیٹری آپٹیمائزیشن کو غیر فعال کریں۔"
                            else
                                "NoorPulse uses native Android AlarmManager to trigger scheduled Adhan and notifications on time even when the screen is locked. On some Android OEM devices, exempting NoorPulse from aggressive battery killers ensures 100% timely alerts.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isUrdu) "درست الارم کی اجازت چیک کریں" else "Check Exact Alarm Permission")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun PrayerToggleRow(
    name: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 14.sp)
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}
