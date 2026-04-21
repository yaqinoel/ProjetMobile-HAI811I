package com.example.traveling.features.travelshare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveling.ui.theme.*

// ─── 数据模型与常量 ───

data class FilterItem(val id: String, val label: String)
data class CategoryItem(val id: String, val label: String, val image: String, val count: String)
data class TrendingItem(val label: String, val type: String, val icon: ImageVector, val color: Color)
data class DestItem(val name: String, val region: String, val img: String)

val PLACE_TYPES = listOf(
    FilterItem("all", "Tous"), FilterItem("nature", "Nature"), FilterItem("museum", "Musée"),
    FilterItem("street", "Rue ancienne"), FilterItem("temple", "Temple"), FilterItem("monument", "Monument"), FilterItem("park", "Jardin")
)

val PERIODS = listOf(
    FilterItem("all", "Toutes"), FilterItem("week", "Cette semaine"), FilterItem("month", "Ce mois"),
    FilterItem("3months", "3 derniers mois"), FilterItem("year", "Cette année")
)

val CATEGORIES = listOf(
    CategoryItem("shansui", "Paysages", "https://images.unsplash.com/photo-1773318901379-aac92fdf5611?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", "3.2k"),
    CategoryItem("gujian", "Architecture", "https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", "4.1k"),
    CategoryItem("dushi", "Urbain", "https://images.unsplash.com/photo-1647067151201-0b37c7555870?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", "2.8k"),
    CategoryItem("yexiao", "Nocturne", "https://images.unsplash.com/photo-1709133332724-2f56232c77eb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", "1.5k"),
    CategoryItem("ziranfeng", "Nature", "https://images.unsplash.com/photo-1770035242840-4e25de3298ee?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", "2.4k"),
    CategoryItem("yuanlin", "Jardins", "https://images.unsplash.com/photo-1586862118451-efc84a66e704?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", "1.1k")
)

val TRENDING = listOf(
    TrendingItem("Cité Interdite", "Lieu", Icons.Default.LocationOn, Color(0xFFB91C1C)),
    TrendingItem("Lac de l'Ouest", "Lieu", Icons.Default.LocationOn, Color(0xFFB91C1C)),
    TrendingItem("Li Xiaofang", "Voyageur", Icons.Default.Person, Color(0xFF7C3AED)),
    TrendingItem("Saison des cerisiers", "Tendance", Icons.Default.TrendingUp, Color(0xFFD97706)),
    TrendingItem("Randonnée", "Tendance", Icons.Default.TrendingUp, Color(0xFFD97706))
)

val RECENT = listOf("Lever soleil Muraille", "Gastronomie Chengdu", "Grottes Mogao", "Passerelle verre Zhangjiajie")

val DESTINATIONS = listOf(
    DestItem("Guilin", "Guangxi", "https://images.unsplash.com/photo-1773318901379-aac92fdf5611?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=300"),
    DestItem("Hangzhou", "Zhejiang", "https://images.unsplash.com/photo-1586862118451-efc84a66e704?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=300"),
    DestItem("Shanghai", "Municipality", "https://images.unsplash.com/photo-1647067151201-0b37c7555870?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=300")
)

// Convertir Destination Firestore en DestItem local
fun com.example.traveling.data.model.Destination.toDestItem() = DestItem(
    name = this.name,
    region = this.country,
    img = this.imageUrl
)


// ─── 核心界面 ───

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    isAnonymous: Boolean = false,
    exploreViewModel: ExploreViewModel = viewModel()
) {
    val firestoreDestinations by exploreViewModel.destinations.collectAsState()
    val displayDestinations = if (firestoreDestinations.isNotEmpty()) {
        firestoreDestinations.map { it.toDestItem() }
    } else {
        DESTINATIONS
    }
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("all") }
    var selectedPeriod by remember { mutableStateOf("all") }
    var radiusKm by remember { mutableStateOf(50f) }

    val filteredTrending = if (searchQuery.isNotEmpty()) {
        TRENDING.filter { it.label.contains(searchQuery, ignoreCase = true) }
    } else {
        TRENDING
    }

    Column(modifier = Modifier.fillMaxSize().background(ExplorePageBg)) {

        // 顶部 Header (含搜索与筛选)
        Surface(
            color = ExploreCardBg.copy(alpha = 0.95f),
            shadowElevation = if (showFilters) 4.dp else 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题与筛选开关
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Explorer", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ExploreStoneText)
                    IconButton(
                        onClick = { showFilters = !showFilters },
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (showFilters) ExploreRedPrimary else ExploreRedLight, RoundedCornerShape(8.dp))
                            .border(1.dp, ExploreRedPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Tune, "Filtres", tint = if (showFilters) Color.White else ExploreStoneMuted, modifier = Modifier.size(20.dp))
                    }
                }

                // 搜索框
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(ExploreStoneLight, RoundedCornerShape(8.dp))
                        .border(1.dp, if (searchQuery.isNotEmpty()) ExploreRedPrimary.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(Icons.Default.Search, null, tint = ExploreStoneMuted, modifier = Modifier.padding(start = 12.dp).size(20.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = ExploreStoneText, fontSize = 14.sp),
                        cursorBrush = SolidColor(ExploreRedPrimary),
                        modifier = Modifier.fillMaxWidth().padding(start = 40.dp, end = 80.dp),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Rechercher lieux, voyageurs, tags...", color = ExploreStoneMuted, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )

                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            Box(modifier = Modifier.size(24.dp).background(Color.LightGray, CircleShape).clickable { searchQuery = "" }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Box(modifier = Modifier.size(28.dp).background(ExploreRedLight, RoundedCornerShape(6.dp)).border(1.dp, ExploreRedPrimary.copy(0.1f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Mic, null, tint = ExploreRedPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // 展开的筛选器面板
                AnimatedVisibility(
                    visible = showFilters,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Type
                        Column {
                            Text("Type de lieu", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ExploreStoneMuted, modifier = Modifier.padding(bottom = 6.dp))
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PLACE_TYPES.forEach { type ->
                                    val isSelected = selectedType == type.id
                                    Text(
                                        text = type.label,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else ExploreStoneMuted,
                                        modifier = Modifier
                                            .background(if (isSelected) ExploreRedPrimary else ExploreStoneLight, RoundedCornerShape(6.dp))
                                            .clickable { selectedType = type.id }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        // Period
                        Column {
                            Text("Période", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ExploreStoneMuted, modifier = Modifier.padding(bottom = 6.dp))
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PERIODS.forEach { p ->
                                    val isSelected = selectedPeriod == p.id
                                    Text(
                                        text = p.label,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else ExploreStoneMuted,
                                        modifier = Modifier
                                            .background(if (isSelected) ExploreRedPrimary else ExploreStoneLight, RoundedCornerShape(6.dp))
                                            .clickable { selectedPeriod = p.id }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        // Radius Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rayon de proximité", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ExploreStoneMuted)
                                Text("${radiusKm.toInt()} km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExploreRedPrimary)
                            }
                            Slider(
                                value = radiusKm,
                                onValueChange = { radiusKm = it },
                                valueRange = 5f..500f,
                                colors = SliderDefaults.colors(thumbColor = ExploreRedPrimary, activeTrackColor = ExploreRedPrimary, inactiveTrackColor = ExploreStoneLight)
                            )
                        }
                        // Search by image Button
                        Button(
                            onClick = { /* TODO */ },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFBEB)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFEF3C7))
                        ) {
                            Icon(Icons.Default.ImageSearch, null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Recherche par image", color = Color(0xFFB45309), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // 滚动内容区
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- 未搜索状态：显示最近搜索 ---
            if (searchQuery.isEmpty()) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Recherches récentes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExploreStoneText)
                        Text("Effacer", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ExploreRedPrimary, modifier = Modifier.clickable { })
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RECENT.forEach { s ->
                            Text(
                                text = s,
                                fontSize = 13.sp,
                                color = ExploreStoneMuted,
                                modifier = Modifier
                                    .background(ExploreStoneLight, RoundedCornerShape(6.dp))
                                    .clickable { searchQuery = s }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // --- 搜索建议 / 热门趋势 ---
            Column {
                Text(
                    text = if (searchQuery.isNotEmpty()) "Résultats" else "Tendances",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExploreStoneText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredTrending.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { searchQuery = item.label }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).background(item.color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(item.icon, null, tint = item.color, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ExploreStoneText)
                                Text(item.type, fontSize = 12.sp, color = ExploreStoneMuted)
                            }
                            Icon(Icons.Default.Search, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // --- 未搜索状态：显示分类网格 ---
            if (searchQuery.isEmpty()) {
                Column {
                    Text("Parcourir par catégorie", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExploreStoneText, modifier = Modifier.padding(bottom = 12.dp))

                    // 将类别分组，每行2个
                    CATEGORIES.chunked(2).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { /* 跳转分类 */ }
                                ) {
                                    AsyncImage(model = cat.image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
                                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                        Text(cat.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("${cat.count} photos", color = Color.White.copy(0.7f), fontSize = 11.sp)
                                    }
                                }
                            }
                            // 如果是奇数，用一个空的占据位置
                            if (rowItems.size == 1) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }

                // --- 未搜索状态：热门目的地卡片 ---
                Column {
                    Text("Destinations populaires", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExploreStoneText, modifier = Modifier.padding(bottom = 12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(displayDestinations) { dest ->
                            Card(
                                modifier = Modifier.width(140.dp),
                                colors = CardDefaults.cardColors(containerColor = ExploreCardBg),
                                border = BorderStroke(1.dp, ExploreStoneLight)
                            ) {
                                Column {
                                    AsyncImage(
                                        model = dest.img, contentDescription = null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(96.dp)
                                    )
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(dest.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExploreStoneText)
                                        Text(dest.region, fontSize = 11.sp, color = ExploreStoneMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Explore Screen Preview")
@Composable
fun ExploreScreenPreview() {
    ExploreScreen(isAnonymous = false)
}