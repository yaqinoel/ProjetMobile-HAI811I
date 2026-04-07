package com.example.traveling.features.travelshare

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// ─── 数据模型 ───
data class Story(val id: String, val name: String, val avatar: String, val color: Color, val hasNew: Boolean)

data class PhotoPost(
    val id: String, val imageUrl: String, val location: String, val country: String,
    val date: String, val author: String, val authorAvatar: String, val authorColor: Color,
    val likes: Int, val isLiked: Boolean, val isSaved: Boolean,
    val description: String, val comments: Int, val tags: List<String>
)

// ─── 颜色定义 (匹配 Figma 设计) ───
val PageBg = Color(0xFFFDF8F4)
val CardBg = Color(0xFFFFFBF5)
val RedPrimary = Color(0xFFB91C1C)
val RedLight = Color(0xFFFEF2F2)
val StoneText = Color(0xFF292524)
val StoneMuted = Color(0xFF78716C)

// ─── 模拟数据 ───
val STORIES = listOf(
    Story("1", "Xiaofang", "X", Color(0xFFB91C1C), true),
    Story("2", "Zhiyuan", "Z", Color(0xFF7C3AED), true),
    Story("3", "Wanqing", "W", Color(0xFFD97706), true),
    Story("4", "Minghui", "M", Color(0xFF0D9488), false),
    Story("5", "Yuxuan", "Y", Color(0xFF2563EB), false),
    Story("6", "Zixuan", "Z", Color(0xFFDC2626), true),
)

val INITIAL_PHOTOS = listOf(
    PhotoPost("1", "https://images.unsplash.com/photo-1558507564-c573429b9ceb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800", "Grande Muraille", "Pékin, Chine", "15 mars 2026", "Li Xiaofang", "L", Color(0xFFB91C1C), 1234, false, false, "La Grande Muraille s'étend sur des milliers de kilomètres, majestueuse. Au lever du soleil, la lumière dorée illumine les remparts.", 42, listOf("Grande Muraille", "Lever du soleil", "Monument")),
    PhotoPost("2", "https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800", "Cité Interdite", "Pékin, Chine", "10 mars 2026", "Wang Wanqing", "W", Color(0xFFD97706), 2567, true, true, "Les murs rouges et les tuiles dorées de la Cité Interdite portent 600 ans d'histoire.", 89, listOf("Cité Interdite", "Architecture", "Impérial")),
    PhotoPost("3", "https://images.unsplash.com/photo-1773318901379-aac92fdf5611?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800", "Paysages de Guilin", "Guangxi, Chine", "28 fév 2026", "Zhang Zhiyuan", "Z", Color(0xFF7C3AED), 891, false, false, "Les paysages de Guilin sont les plus beaux du monde.", 35, listOf("Guilin", "Paysage")),
    PhotoPost("4", "https://images.unsplash.com/photo-1770035242840-4e25de3298ee?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800", "Zhangjiajie", "Hunan, Chine", "15 fév 2026", "Chen Minghui", "C", Color(0xFF0D9488), 756, false, false, "Zhangjiajie ressemble à un paradis. Lieu de tournage d'Avatar !", 28, listOf("Zhangjiajie", "Nuages"))
)

// ─── 核心 UI ───
@Composable
fun GalleryScreen(
    isAnonymous: Boolean = false,
    onOpenNotifications: () -> Unit = {}
    ) {
    var viewMode by remember { mutableStateOf("list") } // "list", "grid", "map"
    var photos by remember { mutableStateOf(INITIAL_PHOTOS) }

    Column(modifier = Modifier.fillMaxSize().background(PageBg)) {
        // 1. 顶部导航栏
        HeaderBar(
            viewMode = viewMode,
            onOpenNotifications = onOpenNotifications,
            onViewModeChanged = { viewMode = it },
            isAnonymous = isAnonymous,
            onShuffle = { photos = photos.shuffled() }
        )

        // 2. Stories 动态区域 (如果是地图模式则隐藏)
        if (viewMode != "map") {
            StoriesRow(isAnonymous = isAnonymous)
            Divider(color = Color.Black.copy(alpha = 0.05f))
        }

        // 3. 内容展示区 (带淡入淡出动画)
        Crossfade(targetState = viewMode, label = "ViewMode") { mode ->
            when (mode) {
                "list" -> PhotoListView(photos = photos, onLike = { /* TODO */ }, onSave = { /* TODO */ })
                "grid" -> PhotoGridView(photos = photos)
                "map" -> MapView(photos = photos, onSelectPhoto = { /* TODO */ })
            }
        }
    }
}

// ─── 顶部导航栏 ───
@Composable
private fun HeaderBar(
    viewMode: String,
    onOpenNotifications: () -> Unit,
    onViewModeChanged: (String) -> Unit,
    isAnonymous: Boolean,
    onShuffle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(CardBg).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Galerie", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StoneText)
            Text(if (viewMode == "map") "Vue carte" else "Découvrez les merveilles", fontSize = 11.sp, color = StoneMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 随机按钮
            IconButton(onClick = onShuffle, modifier = Modifier.size(36.dp).background(RedLight, RoundedCornerShape(8.dp)).border(1.dp, RedPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))) {
                Icon(Icons.Default.Shuffle, contentDescription = "Aléatoire", tint = RedPrimary, modifier = Modifier.size(18.dp))
            }
            // 视图切换器
            ViewToggle(viewMode = viewMode, onSetViewMode = onViewModeChanged)

            // 通知铃铛 (登录状态可见)
            if (!isAnonymous) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(RedLight, RoundedCornerShape(8.dp))
                        .border(1.dp, RedPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable { onOpenNotifications()},
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = RedPrimary, modifier = Modifier.size(18.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape).align(Alignment.TopEnd).offset((-6).dp, 6.dp)) // 红点
                }
            }
        }
    }
}

// ─── 视图切换器 ───
@Composable
private fun ViewToggle(viewMode: String, onSetViewMode: (String) -> Unit) {
    Row(modifier = Modifier.background(Color(0xFFF5F5F4), RoundedCornerShape(8.dp)).padding(2.dp)) {
        listOf("list" to Icons.Default.ViewList, "grid" to Icons.Default.GridView, "map" to Icons.Default.Map).forEach { (mode, icon) ->
            val isSelected = viewMode == mode
            Box(
                modifier = Modifier.size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onSetViewMode(mode) },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = mode, tint = if (isSelected) RedPrimary else StoneMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─── 顶部 Stories 横向列表 ───
@Composable
private fun StoriesRow(isAnonymous: Boolean) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(CardBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isAnonymous) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(60.dp).background(RedLight, CircleShape).border(2.dp, RedPrimary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 24.sp, color = RedPrimary.copy(alpha = 0.6f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Ajouter", fontSize = 10.sp, color = StoneMuted, fontWeight = FontWeight.Medium)
                }
            }
        }
        items(STORIES) { story ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 头像边框 (渐变代表有新动态)
                val borderBrush = if (story.hasNew) Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFDC2626))) else Brush.linearGradient(listOf(Color.LightGray, Color.LightGray))
                Box(modifier = Modifier.size(60.dp).background(borderBrush, CircleShape).padding(2.5.dp)) {
                    Box(modifier = Modifier.fillMaxSize().background(CardBg, CircleShape).padding(2.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(story.color, CircleShape), contentAlignment = Alignment.Center) {
                            Text(story.avatar, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(story.name, fontSize = 10.sp, color = StoneText, fontWeight = if (story.hasNew) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ─── List 列表视图 (详细卡片) ───
@Composable
private fun PhotoListView(photos: List<PhotoPost>, onLike: (String) -> Unit, onSave: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(photos) { photo ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // 1. 卡片头部 (作者、位置)
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).background(photo.authorColor, CircleShape), contentAlignment = Alignment.Center) {
                            Text(photo.authorAvatar, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(photo.author, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StoneText)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = StoneMuted, modifier = Modifier.size(12.dp))
                                Text("${photo.location}, ${photo.country}", fontSize = 11.sp, color = StoneMuted, maxLines = 1)
                            }
                        }
                        IconButton(onClick = { }, modifier = Modifier.size(32.dp).background(RedLight, RoundedCornerShape(8.dp))) {
                            Icon(Icons.Outlined.Navigation, contentDescription = "Naviguer", tint = RedPrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // 2. 主图片
                    AsyncImage(
                        model = photo.imageUrl, contentDescription = photo.description, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                    )

                    // 3. 互动按钮、文字与标签
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (photo.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Like", tint = if (photo.isLiked) Color.Red else StoneText, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${photo.likes}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", tint = StoneText, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${photo.comments}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = StoneText, modifier = Modifier.size(20.dp))
                            }
                            Icon(if (photo.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = "Save", tint = if (photo.isSaved) Color(0xFFD97706) else StoneText, modifier = Modifier.size(22.dp))
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(text = photo.description, fontSize = 13.sp, color = StoneText, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            photo.tags.forEach { tag ->
                                Text("#$tag", fontSize = 11.sp, color = RedPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.background(RedLight, RoundedCornerShape(12.dp)).border(1.dp, RedPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(photo.date, fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

// ─── Grid 网格视图 ───
@Composable
private fun PhotoGridView(photos: List<PhotoPost>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(photos) { photo ->
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp))) {
                AsyncImage(model = photo.imageUrl, contentDescription = photo.location, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                // 底部渐变黑色背景
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.8f)))))

                // 右上角点赞按钮
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(if (photo.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Like", tint = if (photo.isLiked) Color.Red else Color.White, modifier = Modifier.size(14.dp))
                }

                // 左下角文字信息
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(photo.location, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(photo.country, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Mode Connecté (已登录状态)")
@Composable
fun GalleryScreenPreview() {
    // 预览已登录用户的界面
    GalleryScreen(isAnonymous = false)
}

@Preview(showBackground = true, name = "Mode Anonyme (匿名状态)")
@Composable
fun GalleryScreenAnonymousPreview() {
    // 预览匿名用户的界面（你会发现顶部的加号和通知小铃铛不见了）
    GalleryScreen(isAnonymous = true)
}