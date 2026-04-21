package com.example.traveling.features.travelshare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.traveling.data.model.PhotoDetail
import com.example.traveling.ui.theme.*

// 主页面入口 (负责状态管理)

@Composable
fun PhotoDetailScreen(
    photoId: String,
    onBack: () -> Unit = {},
    viewModel: PhotoDetailViewModel = viewModel()
) {
    // 收集 ViewModel 的状态
    val uiState by viewModel.uiState.collectAsState()

    // 当 photoId 改变时（或首次进入页面时），通知 ViewModel 加载数据
    LaunchedEffect(photoId) {
        viewModel.loadPhoto(photoId)
    }

    // 根据不同的状态渲染对应的 UI
    when (val state = uiState) {
        is PhotoDetailUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(PageBg),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RedPrimary)
            }
        }
        is PhotoDetailUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().background(PageBg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Stone400, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(text = state.message, color = Stone600, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("Retour")
                    }
                }
            }
        }
        is PhotoDetailUiState.Success -> {
            // 数据加载成功，渲染真实 UI
            PhotoDetailContent(
                photo = state.photo,
                onBack = onBack
            )
        }
    }
}

// ─── 2. 纯展示组件 (只负责画出 PhotoDetail 数据) ───

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoDetailContent(
    photo: PhotoDetail,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { photo.imageUrls.size })

    Scaffold(
        containerColor = PageBg,
        bottomBar = { BottomActionBar(photo = photo) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // --- 顶部图片轮播区域 ---
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    AsyncImage(
                        model = photo.imageUrls[page],
                        contentDescription = "Image ${page + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(top = 48.dp, start = 16.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }

                if (photo.imageUrls.size > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 48.dp, end = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${photo.imageUrls.size}",
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // --- 详情内容区域 ---
            Column(modifier = Modifier.padding(16.dp)) {

                // 作者信息
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(photo.authorColor, CircleShape), contentAlignment = Alignment.Center) {
                        Text(photo.authorAvatar, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(photo.author, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Stone800)
                        Text(photo.date, fontSize = 12.sp, color = StoneMuted)
                    }
                    Surface(
                        onClick = { /* TODO */ },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.2f))
                    ) {
                        Text("Suivre", fontSize = 12.sp, color = RedPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = StoneBorder)
                Spacer(Modifier.height(16.dp))

                // 位置信息
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Stone100)
                        .clickable { /* TODO */ }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${photo.location}, ${photo.country}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Stone800)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Stone400, modifier = Modifier.size(16.dp))
                }

                Spacer(Modifier.height(16.dp))

                // 描述正文
                Text(text = photo.description, fontSize = 15.sp, color = Stone800, lineHeight = 24.sp)

                Spacer(Modifier.height(16.dp))

                // 标签
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    photo.tags.forEach { tag ->
                        Text(
                            text = "#$tag", fontSize = 13.sp, color = RedPrimary, fontWeight = FontWeight.Medium,
                            modifier = Modifier.background(Color(0xFFFEF2F2), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─── 3. 子组件：底部固定互动栏 ───

@Composable
private fun BottomActionBar(photo: PhotoDetail) {
    Surface(color = CardBg, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).height(40.dp).background(Stone100, RoundedCornerShape(20.dp)).clickable { /* TODO */ }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = Stone400, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ajouter un commentaire...", fontSize = 13.sp, color = Stone400)
            }

            Spacer(Modifier.width(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                    Icon(if (photo.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Like", tint = if (photo.isLiked) Color.Red else Stone800, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${photo.likes}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Stone800)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", tint = Stone800, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${photo.comments}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Stone800)
                }
                Icon(
                    if (photo.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (photo.isSaved) AmberAccent else Stone800,
                    modifier = Modifier.size(24.dp).clickable { }
                )
            }
        }
    }
}

// ─── 4. 预览 (测试专用) ───
// 这里的 Preview 不直接调用主 Screen（因为没法轻易 mock ViewModel）
@Preview(showBackground = true)
@Composable
fun PhotoDetailContentPreview() {
    val mockData = PhotoDetail(
        id = "1",
        imageUrls = listOf("https://images.unsplash.com/photo-1558507564-c573429b9ceb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800"),
        location = "Grande Muraille", country = "Pékin, Chine", date = "15 mars 2026",
        author = "Li Xiaofang", authorAvatar = "L", authorColor = RedPrimary,
        likes = 1234, isLiked = true, isSaved = false,
        description = "Test description.", comments = 42, tags = listOf("Test")
    )
    PhotoDetailContent(photo = mockData, onBack = {})
}