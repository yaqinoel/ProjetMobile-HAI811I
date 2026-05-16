package com.example.traveling.features.travelshare

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import coil.request.ImageRequest
import com.example.traveling.features.travelshare.PhotoPostUi
import com.example.traveling.ui.theme.*

private val PHOTO_COORDS = mapOf(
    "1" to LatLng(40.43, 116.57),
    "2" to LatLng(39.92, 116.40),
    "3" to LatLng(25.27, 110.29),
    "4" to LatLng(29.32, 110.43),
    "5" to LatLng(30.24, 120.14),
    "6" to LatLng(31.24, 121.49)
)

@Composable
fun MapView(
    photos: List<PhotoPostUi>,
    onSelectPhoto: (String) -> Unit
) {
    var selectedPin by remember { mutableStateOf<String?>(null) }
    val selectedPhoto = photos.find { it.id == selectedPin }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }

    val fallbackCenter = LatLng(43.6108, 3.8767)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(fallbackCenter, 12f)
    }

    val uiSettings by remember {
        mutableStateOf(MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false))
    }
    val mapProperties = MapProperties(
        mapType = MapType.NORMAL,
        isMyLocationEnabled = hasLocationPermission
    )

    fun moveToCurrentLocation() {
        // utilisé au démarrage et par le bouton de recentrage
        val location = context.lastKnownLocationOrNull() ?: return
        coroutineScope.launch {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    13f
                ),
                650
            )
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            moveToCurrentLocation()
        }
    }

    fun requestOrMoveToCurrentLocation() {
        if (context.hasLocationPermission()) {
            hasLocationPermission = true
            moveToCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        // la carte s'ouvre sur la position de l'utilisateur si l'autorisation existe
        requestOrMoveToCurrentLocation()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
            properties = mapProperties,
            onMapClick = { selectedPin = null }
        ) {

            photos.forEach { photo ->
                // les anciens posts sans coordonnées gardent un fallback pour ne pas casser la carte
                val coord = if (photo.latitude != null && photo.longitude != null) {
                    LatLng(photo.latitude, photo.longitude)
                } else {
                    PHOTO_COORDS[photo.id] ?: LatLng(35.0, 105.0)
                }
                val isSelected = selectedPin == photo.id

                MarkerComposable(
                    state = MarkerState(position = coord),
                    onClick = {
                        selectedPin = if (isSelected) null else photo.id

                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLng(coord),
                                500
                            )
                        }
                        true
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 36.dp else 28.dp)
                            .offset(y = (-14).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Pin",
                            tint = if (isSelected) Color(0xFFDC2626) else RedPrimary,
                            modifier = Modifier.fillMaxSize()
                        )
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photo.imageUrl)
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(if (isSelected) 16.dp else 12.dp)
                                .offset(y = (-4).dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) }
                },
                modifier = Modifier.size(40.dp),
                containerColor = Color.White.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.Add, "Zoom In", tint = StoneText)
            }
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomOut()) }
                },
                modifier = Modifier.size(40.dp),
                containerColor = Color.White.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.Remove, "Zoom Out", tint = StoneText)
            }
            FloatingActionButton(
                onClick = {
                    requestOrMoveToCurrentLocation()
                },
                modifier = Modifier.size(40.dp),
                containerColor = Color.White.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.MyLocation, "Ma position", tint = StoneText)
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 4.dp
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Map, null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("${photos.size} pins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StoneText)
            }
        }

        AnimatedVisibility(
            visible = selectedPhoto != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedPhoto?.let { photo ->
                val displayTitle = photo.title.ifBlank { photo.location }
                val displayLocation = listOf(photo.location, photo.country)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" · ")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .shadow(14.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardBg)
                        .clickable { onSelectPhoto(photo.id) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model = photo.imageUrl,
                                    contentDescription = displayTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    displayTitle,
                                    color = StoneText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (photo.visibility == "group" && !photo.groupName.isNullOrBlank()) {
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        photo.groupName.orEmpty(),
                                        color = Color(0xFFD97706),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .widthIn(max = 120.dp)
                                            .background(Color(0xFFFFF7ED), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = RedPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        displayLocation.ifBlank { "Lieu inconnu" },
                                        color = StoneMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    photo.description,
                                    color = Stone500,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { selectedPin = null },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Stone100, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Stone500, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row {
                            Button(
                                onClick = { onSelectPhoto(photo.id) },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                            ) {
                                Text("Voir les détails", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Context.hasLocationPermission(): Boolean {
    val hasFine = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return hasFine || hasCoarse
}

private fun Context.lastKnownLocationOrNull(): Location? {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    return providers
        .filter { provider -> runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false) }
        .mapNotNull { provider ->
            runCatching {
                locationManager.getLastKnownLocation(provider)
            }.getOrNull()
        }
        .maxByOrNull { it.time }
}
