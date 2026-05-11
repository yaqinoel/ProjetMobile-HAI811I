package com.example.traveling.features.travelshare

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
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import coil.request.ImageRequest
import com.example.traveling.features.travelshare.model.PhotoPostUi
import com.example.traveling.ui.theme.*

// 将坐标映射为 Google Maps 专用的 LatLng 对象
private val PHOTO_COORDS = mapOf(
    "1" to LatLng(40.43, 116.57), // 北京长城
    "2" to LatLng(39.92, 116.40), // 北京故宫
    "3" to LatLng(25.27, 110.29), // 桂林
    "4" to LatLng(29.32, 110.43), // 张家界
    "5" to LatLng(30.24, 120.14), // 杭州西湖
    "6" to LatLng(31.24, 121.49)  // 上海外滩
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

    // 初始视角定位在中国中心点
    val chinaCenter = LatLng(35.8617, 104.1954)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(chinaCenter, 4f)
    }

    // 地图 UI 设置：隐藏默认的缩放按钮，我们用自己设计的
    val uiSettings by remember {
        mutableStateOf(MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false))
    }
    val mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds() // 彻底解决溢出遮挡顶部栏的问题
    ) {
        // --- 1. Google Maps 原生图层 ---
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
            properties = mapProperties,
            onMapClick = { selectedPin = null } // 点击空白处关闭详情页
        ) {
            // 在地图上绘制所有的自定义图钉
            photos.forEach { photo ->
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
                        // 点击图钉时，平滑移动相机到图钉位置
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
                            .size(if (isSelected) 36.dp else 28.dp) // 选中时稍微变大
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
                            // 使用 ImageRequest 并强制关闭硬件加速
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

        // 悬浮控制器 (自定义的缩放与定位按钮)
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
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(chinaCenter, 4f))
                    }
                },
                modifier = Modifier.size(40.dp),
                containerColor = Color.White.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.MyLocation, "Reset", tint = StoneText)
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

        // 底部滑动详情页 (Bottom Sheet)
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
