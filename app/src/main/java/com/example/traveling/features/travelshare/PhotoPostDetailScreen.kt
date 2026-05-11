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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext

// 导入数据模型
import com.example.traveling.core.utils.openNavigationToPlace
import com.example.traveling.features.travelshare.model.PhotoPostDetailUi
import com.example.traveling.ui.components.UserAvatar
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

// 主页面入口 (状态与路由管理)

@Composable
fun PhotoPostDetailScreen(
    photoId: String,
    isAnonymous: Boolean = false,
    onBack: () -> Unit = {},
    onNavigateLogin: () -> Unit = {},
    onNavigateRegister: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    viewModel: PhotoPostDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 当 photoId 传入时，触发 ViewModel 加载真实数据
    LaunchedEffect(photoId) {
        viewModel.loadPhoto(photoId)
    }

    when (val state = uiState) {
        is PhotoPostDetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(PageBg), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedPrimary)
            }
        }
        is PhotoPostDetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().background(PageBg), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Stone400, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(text = state.message, color = Stone600, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)) {
                        Text("Retour")
                    }
                }
            }
        }
        is PhotoPostDetailUiState.Success -> {
            // 数据成功加载，把 PhotoDetail 喂给纯 UI 组件
            PhotoPostDetailContent(
                photo = state.photo,
                isAnonymous = isAnonymous,
                onBack = onBack,
                onNavigateLogin = onNavigateLogin,
                onNavigateRegister = onNavigateRegister,
                onAuthorClick = onAuthorClick,
                viewModel = viewModel
            )
        }
    }
}

// 纯展示组件

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PhotoPostDetailContent(
    photo: PhotoPostDetailUi,
    isAnonymous: Boolean,
    onBack: () -> Unit,
    onNavigateLogin: () -> Unit,
    onNavigateRegister: () -> Unit,
    onAuthorClick: (String) -> Unit,
    viewModel: PhotoPostDetailViewModel
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { photo.imageUrls.size })
    var newComment by remember { mutableStateOf("") }
    var showActionsMenu by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val displayTitle = photo.title.ifBlank { photo.location }
    val bodyText = photo.description.takeIf { it.isNotBlank() && it != displayTitle }

    Scaffold(
        containerColor = PageBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            // ─── 顶部导航栏 (Figma 中的 Header) ───
            Surface(
                color = PageBg.copy(alpha = 0.95f), // 稍微带点透明度的毛玻璃效果
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 返回按钮 (浅红色底)
                        Surface(
                            onClick = onBack,
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Stone600, modifier = Modifier.size(18.dp))
                            }
                        }

                        // 顶部作者头像与名字
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable(enabled = photo.authorId.isNotBlank()) {
                                onAuthorClick(photo.authorId)
                            }
                        ) {
                            UserAvatar(
                                avatarUrl = photo.authorAvatarUrl,
                                fallbackText = photo.authorAvatar,
                                backgroundColor = photo.authorColor,
                                modifier = Modifier.size(32.dp),
                                textSize = 12.sp
                            )
                            Column {
                                Text(photo.author, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)
                                Text(photo.date, fontSize = 10.sp, color = Stone400)
                            }
                        }
                    }

                    Box {
                        Surface(
                            onClick = { showActionsMenu = !showActionsMenu },
                            shape = RoundedCornerShape(8.dp),
                            color = Stone100,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Stone600, modifier = Modifier.size(18.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Signaler la photo") },
                                leadingIcon = { Icon(Icons.Outlined.Flag, null, tint = Color(0xFFDC2626)) },
                                onClick = {
                                    showActionsMenu = false
                                    showReportSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Suivre l'auteur") },
                                leadingIcon = { Icon(Icons.Outlined.PersonAdd, null, tint = RedPrimary) },
                                onClick = {
                                    showActionsMenu = false
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Auteur suivi.") }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Voir photos similaires") },
                                leadingIcon = { Icon(Icons.Outlined.Collections, null, tint = Stone600) },
                                onClick = {
                                    showActionsMenu = false
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Photos similaires activées.") }
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // ─── 底部悬浮评论框 (Figma 中的 Bottom Input) ───
            Surface(
                color = PageBg,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAnonymous) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Connectez-vous pour commenter", color = Stone500, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = onNavigateRegister, shape = RoundedCornerShape(8.dp)) {
                            Text("Créer")
                        }
                        Button(onClick = onNavigateLogin, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)) {
                            Text("Login")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 输入框
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(Stone100, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = newComment,
                                    onValueChange = { newComment = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Stone800),
                                    decorationBox = { innerTextField ->
                                        if (newComment.isEmpty()) {
                                            Text("Ecrivez votre commentaire...", color = Stone400, fontSize = 13.sp)
                                        }
                                        innerTextField()
                                    }
                                )
                                Icon(Icons.Outlined.Mic, contentDescription = "Voice", tint = Stone400, modifier = Modifier.size(16.dp))
                            }
                        }

                        // 发送按钮 (红色方形)
                        Surface(
                            onClick = {
                                if (newComment.isNotBlank()) {
                                    viewModel.addComment(newComment)
                                    newComment = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = RedDark,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ─── 1. 主图与多图轮播区 (比例 4:3) ───
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    AsyncImage(
                        model = photo.imageUrls[page],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 多图指示器
                if (photo.imageUrls.size > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${pagerState.currentPage + 1} / ${photo.imageUrls.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ─── 2. 互动操作区 (点赞/评论数) ───
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            viewModel.toggleLike()
                        }
                    ) {
                        Icon(if (photo.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (photo.isLiked) Color.Red else Stone600, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${photo.likes}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Stone600, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        // 👈 这里动态读取了真实的评论列表长度
                        Text("${photo.commentsList.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)
                    }
                }
                Icon(
                    if (photo.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    null,
                    tint = if (photo.isSaved) AmberAccent else Stone600,
                    modifier = Modifier.size(22.dp).clickable { viewModel.toggleSave() }
                )
            }

            HorizontalDivider(color = StoneBorder)

            // ─── 3. 描述正文与标签 ───
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = displayTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Stone800,
                    lineHeight = 26.sp
                )
                Spacer(Modifier.height(8.dp))
                if (bodyText != null) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Stone800)) {
                                append(photo.author)
                            }
                            append(" ")
                            append(bodyText)
                        },
                        fontSize = 14.sp,
                        color = Stone800,
                        lineHeight = 22.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    photo.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFEE2E2))
                        ) {
                            Text("#$tag", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = RedDark, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ─── 4. 详细行程卡片 (Info Card) ───
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFFBEB), // 浅黄色底
                    border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val precisionLabel = if (photo.locationPrecision == "approx") {
                            "Zone approximative"
                        } else {
                            "Position exacte"
                        }
                        // 👈 这里使用了新增的 lat 和 lng
                        InfoRow(Icons.Outlined.Place, Color(0xFFFEE2E2), RedDark, photo.location, "${photo.country}\n$precisionLabel\n${photo.lat}, ${photo.lng}")
                        InfoRow(Icons.Outlined.CalendarToday, Color(0xFFFEF3C7), Color(0xFFB45309), "Date", photo.date)
                        // 👈 这里使用了新增的 howToGetThere
                        InfoRow(Icons.Outlined.NearMe, Color(0xFFF3E8FF), Color(0xFF7E22CE), "Comment s'y rendre", photo.howToGetThere)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ─── 5. 导航操作双按钮 ───
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val opened = openNavigationToPlace(
                                context = context,
                                placeName = photo.location,
                                latitude = photo.lat,
                                longitude = photo.lng
                            )
                            if (!opened) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Aucune application de carte disponible.")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedDark)
                    ) {
                        Icon(Icons.Outlined.NearMe, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Google Maps", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { coroutineScope.launch { snackbarHostState.showSnackbar("${photo.location} ajouté à TravelPath.") } },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)) // Amber
                    ) {
                        Icon(Icons.Outlined.Route, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("TravelPath", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, StoneBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Connexion avec TravelPath", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Stone800)
                        Text("Ce lieu peut être ajouté comme étape obligatoire lors de la génération d'un parcours.", fontSize = 12.sp, color = Stone500, lineHeight = 18.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { coroutineScope.launch { snackbarHostState.showSnackbar("${photo.location} sera utilisé dans le prochain parcours.") } },
                                label = { Text("Utiliser ce lieu") },
                                leadingIcon = { Icon(Icons.Outlined.Route, null, modifier = Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = { coroutineScope.launch { snackbarHostState.showSnackbar("Filtre photos similaires activé.") } },
                                label = { Text("Photos similaires") },
                                leadingIcon = { Icon(Icons.Outlined.Collections, null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ─── 6. 真实的评论列表 ───
                Text("Commentaires (${photo.commentsList.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)
                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 👈 动态遍历 viewModel 传过来的评论列表
                    photo.commentsList.forEach { comment ->
                        Row(verticalAlignment = Alignment.Top) {
                            UserAvatar(
                                avatarUrl = comment.avatarUrl,
                                fallbackText = comment.avatar,
                                backgroundColor = comment.color,
                                modifier = Modifier.size(32.dp),
                                textSize = 12.sp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(comment.author, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Stone800)
                                    Text(comment.date, fontSize = 10.sp, color = Stone400)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(comment.text, fontSize = 13.sp, color = Stone600, lineHeight = 18.sp)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                                    Icon(Icons.Outlined.ThumbUp, null, tint = Stone400, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${comment.likes}", fontSize = 11.sp, color = Stone400)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp)) // 防止列表被底部的输入框遮挡
            }
        }
    }

    if (showReportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReportSheet = false },
            containerColor = CardBg
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
                Text("Signaler cette photo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Stone800)
                Spacer(Modifier.height(8.dp))
                Text("Choisissez la raison du signalement. Cette action sera enregistrée dans Firestore plus tard.", fontSize = 13.sp, color = Stone500, lineHeight = 19.sp)
                Spacer(Modifier.height(16.dp))
                listOf("Contenu inapproprié", "Information de lieu incorrecte", "Spam ou publicité", "Droits d'image").forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            showReportSheet = false
                            viewModel.reportPost(reason = reason)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Signalement enregistré : $reason") }
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Flag, null, tint = RedPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(reason, fontSize = 14.sp, color = Stone800)
                    }
                }
            }
        }
    }
}

// ─── 辅助组件：信息行 (用于黄色交通卡片内部) ───
@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, iconBg: Color, iconTint: Color, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = Stone400, lineHeight = 18.sp)
        }
    }
}
