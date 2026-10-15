package com.example.ui.dialogs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.PresetCities
import com.example.model.LocationData
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLocation by viewModel.location.collectAsState()
    val isLocating by viewModel.isLocating.collectAsState()
    val isUrdu = viewModel.language.collectAsState().value == "ur"

    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Preset Cities, 1: Custom Coordinates

    // Custom coord states
    var customCity by remember { mutableStateOf(currentLocation.cityName) }
    var customCountry by remember { mutableStateOf(currentLocation.countryName) }
    var customLat by remember { mutableStateOf(currentLocation.latitude.toString()) }
    var customLon by remember { mutableStateOf(currentLocation.longitude.toString()) }

    // Permission launcher for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.refreshCurrentLocation(
                onSuccess = { onDismiss() },
                onError = { err -> errorMessage = err }
            )
        } else {
            errorMessage = if (isUrdu) "لوکیشن کی اجازت نہیں دی گئی" else "Location permission denied. Please select your city manually."
        }
    }

    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            PresetCities.CITIES
        } else {
            PresetCities.CITIES.filter {
                it.cityName.contains(searchQuery, ignoreCase = true) ||
                        it.countryName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUrdu) "مقام منتخب کریں" else "Select Location",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // GPS Auto Detect Button
                Button(
                    onClick = {
                        val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        val coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

                        if (fineCheck == PackageManager.PERMISSION_GRANTED || coarseCheck == PackageManager.PERMISSION_GRANTED) {
                            viewModel.refreshCurrentLocation(
                                onSuccess = { onDismiss() },
                                onError = { err -> errorMessage = err }
                            )
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_detect_gps"),
                    enabled = !isLocating
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isUrdu) "تلاش جاری ہے..." else "Detecting GPS Location...")
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = "GPS", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isUrdu) "خودکار GPS مقام حاصل کریں" else "Detect My Location (GPS)")
                    }
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Preset vs Custom Coordinates
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(if (isUrdu) "شہروں کی فہرست" else "Cities List") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(if (isUrdu) "کوآرڈینیٹس" else "Coordinates") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        placeholder = { Text(if (isUrdu) "شہر کا نام تلاش کریں..." else "Search city (e.g. Lahore, Karachi)...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_search_city")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cities List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCities) { city ->
                            val isCurrent = city.cityName == currentLocation.cityName
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setLocation(city)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = city.cityName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = city.countryName,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    if (isCurrent) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Custom coordinates entry
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = customCity,
                                onValueChange = { customCity = it },
                                label = { Text(if (isUrdu) "شہر کا نام" else "City Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = customCountry,
                                onValueChange = { customCountry = it },
                                label = { Text(if (isUrdu) "ملک" else "Country") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = customLat,
                                onValueChange = { customLat = it },
                                label = { Text(if (isUrdu) "عرض بلد (Latitude)" else "Latitude (e.g. 24.8607)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = customLon,
                                onValueChange = { customLon = it },
                                label = { Text(if (isUrdu) "طول بلد (Longitude)" else "Longitude (e.g. 67.0011)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Button(
                                onClick = {
                                    val lat = customLat.toDoubleOrNull()
                                    val lon = customLon.toDoubleOrNull()
                                    if (lat != null && lon != null) {
                                        val customLoc = LocationData(
                                            cityName = customCity.ifBlank { "Custom Location" },
                                            countryName = customCountry,
                                            latitude = lat,
                                            longitude = lon,
                                            isAutoDetected = false
                                        )
                                        viewModel.setLocation(customLoc)
                                        onDismiss()
                                    } else {
                                        errorMessage = "Invalid latitude or longitude numbers"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("btn_save_coordinates")
                            ) {
                                Text(if (isUrdu) "کوآرڈینیٹس محفوظ کریں" else "Apply Coordinates")
                            }
                        }
                    }
                }
            }
        }
    }
}
