package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val compassState by viewModel.compassState.collectAsState()
    val qiblaBearing by viewModel.qiblaBearing.collectAsState()
    val qiblaDistanceKm by viewModel.qiblaDistanceKm.collectAsState()
    val location by viewModel.location.collectAsState()
    val isUrdu = viewModel.language.collectAsState().value == "ur"

    // Difference between phone's current heading and Qibla direction
    val headingDiff = remember(compassState.azimuthDegrees, qiblaBearing) {
        var diff = qiblaBearing - compassState.azimuthDegrees
        while (diff < -180.0) diff += 360.0
        while (diff > 180.0) diff -= 360.0
        diff
    }

    // Is the user currently facing the Kaaba (within +/- 4 degrees)?
    val isFacingQibla = abs(headingDiff) <= 4.0

    val animatedDialRotation by animateFloatAsState(
        targetValue = -compassState.azimuthDegrees,
        label = "dial_rotation"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isFacingQibla) Color(0xFF10B981) else Color(0xFF38BDF8),
        label = "ring_color"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isUrdu) "قبلہ نما (قطب نما)" else "Qibla Compass") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_qibla")) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Location & Bearing Info Header
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth().testTag("qibla_info_card")
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
                            text = if (isUrdu) "موجودہ مقام" else "CURRENT LOCATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${location.cityName}, ${location.countryName}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isUrdu) "فاصلہ مکہ معظمہ" else "DISTANCE TO MAKKAH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Text(
                            text = String.format("%,.0f km", qiblaDistanceKm),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Facing Qibla Status Banner
            if (isFacingQibla) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth().testTag("facing_qibla_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Aligned",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUrdu) "ماشاء اللہ! آپ قبلہ رخ ہیں" else "Aligned! You are facing the Holy Kaaba",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val turnDirection = if (headingDiff > 0) {
                        if (isUrdu) "دائیں مڑیں" else "Turn Right"
                    } else {
                        if (isUrdu) "بائیں مڑیں" else "Turn Left"
                    }
                    Text(
                        text = "$turnDirection (${String.format("%.0f°", abs(headingDiff))})",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Rotating Qibla Compass Dial
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(3.dp, ringColor, CircleShape)
                    .testTag("compass_dial"),
                contentAlignment = Alignment.Center
            ) {
                // Dial Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2

                    // Rotate the dial according to phone heading
                    rotate(animatedDialRotation, pivot = center) {
                        // Degree ticks
                        for (i in 0 until 360 step 15) {
                            val angleRad = Math.toRadians(i.toDouble())
                            val tickLen = if (i % 90 == 0) 24f else if (i % 45 == 0) 16f else 8f
                            val startX = center.x + (radius - 12f) * sin(angleRad).toFloat()
                            val startY = center.y - (radius - 12f) * cos(angleRad).toFloat()
                            val endX = center.x + (radius - 12f - tickLen) * sin(angleRad).toFloat()
                            val endY = center.y - (radius - 12f - tickLen) * cos(angleRad).toFloat()

                            drawLine(
                                color = if (i == 0) Color(0xFFEF4444) else Color(0xFF64748B),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = if (i % 90 == 0) 3f else 1.5f
                            )
                        }

                        // Kaaba Qibla Pointer on the dial at target bearing
                        rotate(qiblaBearing.toFloat(), pivot = center) {
                            val arrowPath = Path().apply {
                                moveTo(center.x, center.y - radius + 32f)
                                lineTo(center.x - 14f, center.y - radius + 64f)
                                lineTo(center.x + 14f, center.y - radius + 64f)
                                close()
                            }
                            drawPath(
                                path = arrowPath,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    // Static Top Needle (Phone Direction Indicator)
                    val phoneNeedle = Path().apply {
                        moveTo(center.x, 24f)
                        lineTo(center.x - 8f, 50f)
                        lineTo(center.x + 8f, 50f)
                        close()
                    }
                    drawPath(phoneNeedle, color = ringColor)
                }

                // Center Emblem: Bearing readout and Kaaba Icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(1.dp, ringColor.copy(alpha = 0.5f), CircleShape)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "QIBLA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = String.format("%.0f°", qiblaBearing),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = getCardinalDirection(qiblaBearing),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Heading Readout
            Text(
                text = "${if (isUrdu) "فون رخ" else "Phone Heading"}: ${String.format("%.0f°", compassState.azimuthDegrees)} ${getCardinalDirection(compassState.azimuthDegrees.toDouble())}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Calibration / Sensor warning notice
            if (!compassState.isSensorAvailable) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("sensor_warning_banner")
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Sensor missing", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUrdu) "مقناطیسی سینسر دستیاب نہیں۔ قبلہ کا رخ شمال کے مطابق دکھایا جا رہا ہے۔" else "Magnetic compass sensor is not available on this device. Displaying calculated Qibla bearing.",
                            fontSize = 12.sp,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Calibration", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUrdu) "بہترین درستگی کے لیے فون کو 8 کی شکل میں گھمائیں" else "Wave your phone in a figure-8 motion away from metal objects for best accuracy.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getCardinalDirection(angle: Double): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = ((angle + 11.25) / 22.5).toInt() % 16
    return directions[index]
}
