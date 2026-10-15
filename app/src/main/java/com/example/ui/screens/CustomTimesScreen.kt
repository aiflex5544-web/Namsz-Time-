package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomTimesConfig
import com.example.model.Prayer
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val customTimes by viewModel.customTimes.collectAsState()
    val isUrdu = viewModel.language.collectAsState().value == "ur"

    var isCustomEnabled by remember(customTimes) { mutableStateOf(customTimes.isCustomModeEnabled) }
    var fajrHour by remember(customTimes) { mutableStateOf(customTimes.fajrHour) }
    var fajrMin by remember(customTimes) { mutableStateOf(customTimes.fajrMinute) }
    var dhuhrHour by remember(customTimes) { mutableStateOf(customTimes.dhuhrHour) }
    var dhuhrMin by remember(customTimes) { mutableStateOf(customTimes.dhuhrMinute) }
    var asrHour by remember(customTimes) { mutableStateOf(customTimes.asrHour) }
    var asrMin by remember(customTimes) { mutableStateOf(customTimes.asrMinute) }
    var maghribHour by remember(customTimes) { mutableStateOf(customTimes.maghribHour) }
    var maghribMin by remember(customTimes) { mutableStateOf(customTimes.maghribMinute) }
    var ishaHour by remember(customTimes) { mutableStateOf(customTimes.ishaHour) }
    var ishaMin by remember(customTimes) { mutableStateOf(customTimes.ishaMinute) }

    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isUrdu) "اپنی مرضی کے اوقات" else "Custom Prayer Times") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_custom")) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Mode Toggle Card
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("custom_mode_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCustomEnabled) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isUrdu) "کسٹم اوقات فعال کریں" else "Enable Custom Times",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isCustomEnabled)
                                    (if (isUrdu) "آپ کے طے کردہ کسٹم اوقات گھڑی اور اذان پر لاگو ہیں" else "App is using your custom set times for clock & alarms")
                                else
                                    (if (isUrdu) "خودکار فلکیاتی اوقات فعال ہیں" else "Automatic astronomical calculations are active"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCustomEnabled) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isCustomEnabled,
                            onCheckedChange = { isCustomEnabled = it },
                            modifier = Modifier.testTag("switch_custom_mode")
                        )
                    }
                }
            }

            // Custom Time Selectors for 5 prayers
            item {
                Text(
                    text = if (isUrdu) "تمام نمازوں کے اوقات مقرر کریں" else "Set Timings for Each Prayer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                PrayerTimePickerItem(
                    prayerName = if (isUrdu) Prayer.FAJR.urduName else Prayer.FAJR.englishName,
                    hour = fajrHour,
                    minute = fajrMin,
                    onHourChange = { fajrHour = it },
                    onMinChange = { fajrMin = it },
                    testTagPrefix = "fajr"
                )
            }

            item {
                PrayerTimePickerItem(
                    prayerName = if (isUrdu) Prayer.DHUHR.urduName else Prayer.DHUHR.englishName,
                    hour = dhuhrHour,
                    minute = dhuhrMin,
                    onHourChange = { dhuhrHour = it },
                    onMinChange = { dhuhrMin = it },
                    testTagPrefix = "dhuhr"
                )
            }

            item {
                PrayerTimePickerItem(
                    prayerName = if (isUrdu) Prayer.ASR.urduName else Prayer.ASR.englishName,
                    hour = asrHour,
                    minute = asrMin,
                    onHourChange = { asrHour = it },
                    onMinChange = { asrMin = it },
                    testTagPrefix = "asr"
                )
            }

            item {
                PrayerTimePickerItem(
                    prayerName = if (isUrdu) Prayer.MAGHRIB.urduName else Prayer.MAGHRIB.englishName,
                    hour = maghribHour,
                    minute = maghribMin,
                    onHourChange = { maghribHour = it },
                    onMinChange = { maghribMin = it },
                    testTagPrefix = "maghrib"
                )
            }

            item {
                PrayerTimePickerItem(
                    prayerName = if (isUrdu) Prayer.ISHA.urduName else Prayer.ISHA.englishName,
                    hour = ishaHour,
                    minute = ishaMin,
                    onHourChange = { ishaHour = it },
                    onMinChange = { ishaMin = it },
                    testTagPrefix = "isha"
                )
            }

            // Action Buttons
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val newConfig = CustomTimesConfig(
                                isCustomModeEnabled = isCustomEnabled,
                                fajrHour = fajrHour,
                                fajrMinute = fajrMin,
                                dhuhrHour = dhuhrHour,
                                dhuhrMinute = dhuhrMin,
                                asrHour = asrHour,
                                asrMinute = asrMin,
                                maghribHour = maghribHour,
                                maghribMinute = maghribMin,
                                ishaHour = ishaHour,
                                ishaMinute = ishaMin
                            )
                            viewModel.setCustomTimes(newConfig)
                            statusMessage = if (isUrdu) "اوقات محفوظ ہو گئے اور اذان الرٹس اپ ڈیٹ ہو گئے" else "Custom times saved and Adhan alarms rescheduled!"
                        },
                        modifier = Modifier.weight(1f).testTag("btn_save_custom_times")
                    ) {
                        Text(if (isUrdu) "تبدیلیاں محفوظ کریں" else "Save Changes")
                    }

                    OutlinedButton(
                        onClick = {
                            isCustomEnabled = false
                            val newConfig = customTimes.copy(isCustomModeEnabled = false)
                            viewModel.setCustomTimes(newConfig)
                            statusMessage = if (isUrdu) "خودکار اوقات بحال ہو گئے" else "Reset to Automatic Astronomical Times"
                        },
                        modifier = Modifier.testTag("btn_reset_auto_times")
                    ) {
                        Text(if (isUrdu) "خودکار پر بحال کریں" else "Reset to Auto")
                    }
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PrayerTimePickerItem(
    prayerName: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinChange: (Int) -> Unit,
    testTagPrefix: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = prayerName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                val amPm = if (hour < 12) "AM" else "PM"
                val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                Text(
                    text = String.format("%02d:%02d %s", h12, minute, amPm),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            // Steppers for Hour and Minute
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hour Stepper
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hour", fontSize = 10.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onHourChange((hour - 1 + 24) % 24) },
                            modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_hour_down")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Hour down", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = String.format("%02d", hour),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { onHourChange((hour + 1) % 24) },
                            modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_hour_up")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Hour up", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Minute Stepper
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Min", fontSize = 10.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onMinChange((minute - 1 + 60) % 60) },
                            modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_min_down")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Minute down", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = String.format("%02d", minute),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { onMinChange((minute + 1) % 60) },
                            modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_min_up")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Minute up", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
