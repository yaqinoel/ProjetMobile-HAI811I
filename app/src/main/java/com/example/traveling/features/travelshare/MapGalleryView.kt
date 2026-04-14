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
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.platform.LocalContext
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
    photos: List<PhotoPost>,
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
                val coord = PHOTO_COORDS[photo.id] ?: LatLng(35.0, 105.0)
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
                    // 这是你 Figma 中设计的精美图钉 UI
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

        // --- 2. 悬浮控制器 (自定义的缩放与定位按钮) ---
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

        // --- 3. 底部滑动详情页 (Bottom Sheet) ---
        AnimatedVisibility(
            visible = selectedPhoto != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedPhoto?.let { photo ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .background(Color(0xFFFFFBF5), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(40.dp, 4.dp).background(Color(0xFFE5E7EB), CircleShape))
                        }

                        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))) {
                            AsyncImage(model = photo.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            IconButton(
                                onClick = { selectedPin = null },
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).background(Color.Black.copy(0.4f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                Text(photo.location, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFFFDE047), modifier = Modifier.size(12.dp))
                                    Text("${photo.country} · ${photo.date}", color = Color.White.copy(0.8f), fontSize = 12.sp)
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(photo.description, color = StoneText, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)

                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { onSelectPhoto(photo.id) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                                ) {
                                    Text("Voir les détails", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { /* TODO: 转到行程 */ },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                ) {
                                    Icon(Icons.Outlined.Route, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Itinéraire", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}