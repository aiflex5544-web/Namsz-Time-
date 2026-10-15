package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Prayer
import com.example.model.PrayerTime
import com.example.ui.ActiveScreen
import com.example.ui.MainViewModel
import com.example.ui.components.DigitalText
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun MosqueClockScreen(
    viewModel: MainViewModel,
    onOpenLocationDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTimestamp by viewModel.currentTimestamp.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    val hijriDate by viewModel.hijriDate.collectAsState()
    val location by viewModel.location.collectAsState()
    val design by viewModel.effectiveDesign.collectAsState()
    val isUrdu = viewModel.language.collectAsState().value == "ur"
    val customTimes by viewModel.customTimes.collectAsState()
    val adhanSettings by viewModel.adhanSettings.collectAsState()

    val bgColor = Color(design.backgroundColorHex)
    val clockNumberColor = Color(design.clockNumberColorHex)
    val prayerNameColor = Color(design.prayerNameColorHex)
    val prayerTimeColor = Color(design.prayerTimeColorHex)
    val countdownColor = Color(design.countdownColorHex)
    val highlightColor = Color(design.highlightColorHex)

    // Formatted Time
    val timeCal = remember(currentTimestamp) {
        Calendar.getInstance().apply { timeInMillis = currentTimestamp }
    }
    val timeFormatPattern = if (design.use24Hour) {
        if (design.showSeconds) "HH:mm:ss" else "HH:mm"
    } else {
        if (design.showSeconds) "hh:mm:ss a" else "hh:mm a"
    }
    val formattedTime = remember(currentTimestamp, design.use24Hour, design.showSeconds) {
        SimpleDateFormat(timeFormatPattern, Locale.getDefault()).format(timeCal.time)
    }

    // Gregorian Date Format
    val gregorianDateStr = remember(currentTimestamp, isUrdu) {
        val pattern = if (isUrdu) "EEEE، d MMMM yyyy" else "EEEE, MMMM d, yyyy"
        SimpleDateFormat(pattern, if (isUrdu) Locale("ur") else Locale.ENGLISH).format(Date(currentTimestamp))
    }

    // Next Prayer & Live Countdown calculation from actual timestamps
    val nextPrayer = schedule.nextPrayer
    val diffMs = remember(currentTimestamp, schedule) {
        if (nextPrayer != null) {
            maxOf(0L, nextPrayer.timestampMillis - currentTimestamp)
        } else 0L
    }
    val hoursRem = TimeUnit.MILLISECONDS.toHours(diffMs)
    val minsRem = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
    val secsRem = TimeUnit.MILLISECONDS.toSeconds(diffMs) % 60
    val countdownText = remember(diffMs) {
        String.format("-%02d:%02d:%02d", hoursRem, minsRem, secsRem)
    }

    Scaffold(
        containerColor = bgColor,
        bottomBar = {
            MosqueBottomNavBar(
                viewModel = viewModel,
                onOpenLocationDialog = onOpenLocationDialog,
                isUrdu = isUrdu
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top Status Bar: Location Chip & Custom Mode Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = highlightColor.copy(alpha = 0.8f),
                    modifier = Modifier
                        .testTag("location_pill")
                        .clickable { onOpenLocationDialog() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = countdownColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${location.cityName}, ${location.countryName}",
                            color = prayerTimeColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Custom Times Indicator Tag if active
                if (customTimes.isCustomModeEnabled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = if (isUrdu) "اپنی اوقات" else "Custom Times",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Digital Clock Display
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mosque_digital_clock")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, highlightColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                color = highlightColor.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Big Digital Time Readout
                    DigitalText(
                        text = formattedTime,
                        color = clockNumberColor,
                        fontSize = (42f * design.clockSizeScale),
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.0f,
                        modifier = Modifier.testTag("digital_clock_text")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Gregorian Date
                    Text(
                        text = gregorianDateStr,
                        color = prayerTimeColor.copy(alpha = 0.85f),
                        fontSize = (13f * design.fontSizeScale).sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    // Hijri Date
                    if (design.showHijri) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = hijriDate.format(isUrdu),
                            color = prayerNameColor,
                            fontSize = (13f * design.fontSizeScale).sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Next Prayer & Live Countdown Hero Banner
            if (design.showCountdown && nextPrayer != null) {
                val nextPrayerDisplayName = if (isUrdu) nextPrayer.prayer.urduName else nextPrayer.prayer.englishName
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("next_prayer_banner")
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, countdownColor.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                    color = highlightColor.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isUrdu) "اگلی نماز" else "NEXT PRAYER",
                                color = prayerNameColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = nextPrayerDisplayName,
                                    color = prayerTimeColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${nextPrayer.format(design.use24Hour)})",
                                    color = prayerTimeColor.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Glowing Countdown Readout
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isUrdu) "باقی وقت" else "REMAINING",
                                color = countdownColor.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            DigitalText(
                                text = countdownText,
                                color = countdownColor,
                                fontSize = 20f,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("countdown_text")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timetable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUrdu) "اوقاتِ نماز" else "PRAYER TIMETABLE",
                    color = prayerNameColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = if (isUrdu) "اذان / وقت" else "ADHAN / TIME",
                    color = prayerTimeColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Timetable Rows List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(design.spacing.dpValue.dp)
            ) {
                items(schedule.prayers) { prayerTime ->
                    val isNext = nextPrayer?.prayer == prayerTime.prayer
                    MosquePrayerRow(
                        prayerTime = prayerTime,
                        isNext = isNext,
                        isUrdu = isUrdu,
                        use24h = design.use24Hour,
                        prayerNameColor = prayerNameColor,
                        prayerTimeColor = prayerTimeColor,
                        highlightColor = highlightColor,
                        countdownColor = countdownColor,
                        isAdhanEnabled = adhanSettings.isPrayerEnabled(prayerTime.prayer)
                    )
                }
            }
        }
    }
}

@Composable
private fun MosquePrayerRow(
    prayerTime: PrayerTime,
    isNext: Boolean,
    isUrdu: Boolean,
    use24h: Boolean,
    prayerNameColor: Color,
    prayerTimeColor: Color,
    highlightColor: Color,
    countdownColor: Color,
    isAdhanEnabled: Boolean
) {
    val rowBg = if (isNext) highlightColor else highlightColor.copy(alpha = 0.2f)
    val rowBorder = if (isNext) countdownColor else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_row_${prayerTime.prayer.name}")
            .clip(RoundedCornerShape(10.dp))
            .border(if (isNext) 1.5.dp else 0.5.dp, if (isNext) rowBorder else highlightColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        color = rowBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Indicator dot, Prayer Icon, and Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isNext) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(countdownColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // Prayer Name
                Text(
                    text = if (isUrdu) prayerTime.prayer.urduName else prayerTime.prayer.englishName,
                    color = if (isNext) countdownColor else prayerNameColor,
                    fontSize = 16.sp,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold
                )

                if (!prayerTime.prayer.isMainPrayer) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${if (isUrdu) "مکروہ وقت" else "Sun"})",
                        color = prayerTimeColor.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }

            // Right: Adhan Audio Bell & Scheduled Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (prayerTime.prayer.isMainPrayer) {
                    Icon(
                        imageVector = if (isAdhanEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = "Adhan status",
                        tint = if (isAdhanEnabled) (if (isNext) countdownColor else prayerNameColor.copy(alpha = 0.8f)) else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Digital Time
                DigitalText(
                    text = prayerTime.format(use24h),
                    color = if (isNext) countdownColor else prayerTimeColor,
                    fontSize = 16f,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MosqueBottomNavBar(
    viewModel: MainViewModel,
    onOpenLocationDialog: () -> Unit,
    isUrdu: Boolean
) {
    Surface(
        color = Color(0xFF0F172A),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavButton(
                icon = Icons.Default.LocationOn,
                label = if (isUrdu) "مقام" else "Location",
                testTag = "btn_location",
                onClick = onOpenLocationDialog
            )
            NavButton(
                icon = Icons.Default.Palette,
                label = if (isUrdu) "ڈیزائن" else "Styles",
                testTag = "btn_styles",
                onClick = { viewModel.navigateTo(ActiveScreen.CLOCK_STYLES) }
            )
            NavButton(
                icon = Icons.Default.Schedule,
                label = if (isUrdu) "اپنی اوقات" else "Custom",
                testTag = "btn_custom_times",
                onClick = { viewModel.navigateTo(ActiveScreen.CUSTOM_TIMES) }
            )
            NavButton(
                icon = Icons.Default.Explore,
                label = if (isUrdu) "قبلہ" else "Qibla",
                testTag = "btn_qibla",
                onClick = { viewModel.navigateTo(ActiveScreen.QIBLA_COMPASS) }
            )
            NavButton(
                icon = Icons.Default.Settings,
                label = if (isUrdu) "ترتیبات" else "Settings",
                testTag = "btn_settings",
                onClick = { viewModel.navigateTo(ActiveScreen.SETTINGS) }
            )
        }
    }
}

@Composable
private fun NavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
