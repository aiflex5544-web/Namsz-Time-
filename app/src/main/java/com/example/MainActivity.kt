package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.example.ui.ActiveScreen
import com.example.ui.MainViewModel
import com.example.ui.dialogs.LocationDialog
import com.example.ui.screens.*
import com.example.ui.theme.NoorPulseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on for Mosque Digital Clock Wall Mode
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            val language by viewModel.language.collectAsState()
            val layoutDirection = if (language == "ur") LayoutDirection.Rtl else LayoutDirection.Ltr
            val currentScreen by viewModel.currentScreen.collectAsState()
            val design by viewModel.effectiveDesign.collectAsState()

            var showLocationDialog by remember { mutableStateOf(false) }

            // Adjust in-app screen brightness if customized
            LaunchedEffect(design.brightness) {
                val layoutParams = window.attributes
                layoutParams.screenBrightness = if (design.brightness >= 0f) {
                    design.brightness.coerceIn(0.05f, 1.0f)
                } else {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                window.attributes = layoutParams
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                NoorPulseTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            ActiveScreen.MOSQUE_CLOCK -> {
                                MosqueClockScreen(
                                    viewModel = viewModel,
                                    onOpenLocationDialog = { showLocationDialog = true }
                                )
                            }
                            ActiveScreen.CLOCK_STYLES -> {
                                ClockStylesScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                            ActiveScreen.CUSTOM_TIMES -> {
                                CustomTimesScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                            ActiveScreen.QIBLA_COMPASS -> {
                                QiblaScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                            ActiveScreen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                        }

                        if (showLocationDialog) {
                            LocationDialog(
                                viewModel = viewModel,
                                onDismiss = { showLocationDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reschedule alarms and refresh widget
        com.example.alarm.PrayerAlarmScheduler.scheduleAllPrayers(this)
        com.example.widget.PrayerWidgetProvider.updateAllWidgets(this)
    }
}
