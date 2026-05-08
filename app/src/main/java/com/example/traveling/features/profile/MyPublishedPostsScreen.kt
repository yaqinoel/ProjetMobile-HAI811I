package com.example.traveling.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.features.travelshare.model.PhotoPostUi
import com.example.traveling.features.travelshare.model.toPhotoPostUi
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

private data class ProfilePostUi(
    val post: PhotoPostUi,
    val title: String,
    val visibility: String,
    val groupName: String? = null,
    val linkedToTravelPath: Boolean = false,
    val isReported: Boolean = false,
    val isDraft: Boolean = false,
    val description: String = post.description
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPublishedPostsScreen(
    onBack: () -> Unit = {},
    onOpenPhotoDetail: (String) -> Unit = {}
) {
    val viewModel: MyPublishedPostsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var posts by remember { mutableStateOf(emptyList<ProfilePostUi>()) }
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tous") }
    var editingPostId by remember { mutableStateOf<String?>(null) }
    var deletingPostId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.observeMyPosts()
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is MyPublishedUiState.Success) {
            posts = state.posts.map { it.toProfilePostUi() }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val filtered = remember(posts, query, selectedFilter) {
        posts.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, true) ||
                item.post.location.contains(query, true) ||
                item.post.tags.any { it.contains(query, true) }
            val matchesFilter = when (selectedFilter) {
                "Public" -> item.visibility == "Public"
                "Groupe" -> item.visibility == "Groupe"
                "TravelPath" -> item.linkedToTravelPath
                "Signalés" -> item.isReported
                "Brouillons" -> item.isDraft
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val editing = posts.find { it.post.id == editingPostId }
    val deleting = posts.find { it.post.id == deletingPostId }

    Scaffold(
        containerColor = ProfilePageBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = ProfileCardBg) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Stone800)
                    }
                    Column {
                        Text("Mes publications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StoneText)
                        Text("${posts.size} posts publiés", fontSize = 12.sp, color = StoneMuted)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SearchInput(
                value = query,
                placeholder = "Rechercher par titre, lieu, tag...",
                onValueChange = { query = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Tous", "Public", "Groupe", "TravelPath", "Signalés", "Brouillons").forEach { label ->
                    FilterChip(
                        selected = selectedFilter == label,
                        onClick = { selectedFilter = label },
                        label = { Text(label) }
                    )
                }
            }

            when (val state = uiState) {
                MyPublishedUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RedPrimary)
                    }
                }
                is MyPublishedUiState.Error -> {
                    Text(
                        text = state.message,
                        color = StoneMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                is MyPublishedUiState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(filtered, key = { it.post.id }) { item ->
                            PublishedPostCard(
                                item = item,
                                onView = { onOpenPhotoDetail(item.post.id) },
                                onEdit = { editingPostId = item.post.id },
                                onDelete = { deletingPostId = item.post.id }
                            )
                        }
                    }
                }
            }
        }
    }

    if (editing != null) {
        EditPublishedPostSheet(
            item = editing,
            onDismiss = { editingPostId = null },
            onSave = { updated ->
                val parsedTags = updated.post.tags
                viewModel.updatePost(
                    postId = updated.post.id,
                    title = updated.title,
                    description = updated.description,
                    visibility = updated.visibility,
                    tags = parsedTags
                )
                editingPostId = null
            }
        )
    }

    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deletingPostId = null },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePost(deleting.post.id)
                    deletingPostId = null
                }) { Text("Supprimer", color = Color(0xFFDC2626)) }
            },
            dismissButton = { TextButton(onClick = { deletingPostId = null }) { Text("Annuler") } },
            title = { Text("Supprimer la publication") },
            text = { Text("Cette action retire le post de votre liste locale.") }
        )
    }
}

private fun PhotoPostDocument.toProfilePostUi(): ProfilePostUi {
    val ui = toPhotoPostUi(isLiked = false, isSaved = false)
    val normalizedVisibility = visibility.trim().lowercase()
    return ProfilePostUi(
        post = ui,
        title = title.ifBlank { ui.location },
        visibility = if (normalizedVisibility == "group" || normalizedVisibility == "groupe") "Groupe" else "Public",
        groupName = groupName,
        linkedToTravelPath = isLinkedToTravelPath,
        isReported = status == "reported",
        isDraft = status == "draft",
        description = description.ifBlank { title }
    )
}

@Composable
private fun PublishedPostCard(
    item: ProfilePostUi,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(12.dp)).border(1.dp, StoneBorder, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = item.post.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(10.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = Stone800, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.post.country, color = StoneMuted, fontSize = 12.sp)
                Text(item.post.date, color = StoneMuted, fontSize = 11.sp)
                Text("${item.visibility}${item.groupName?.let { " · $it" } ?: ""}", color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("${item.post.likes} likes · ${item.post.comments} commentaires", color = StoneMuted, fontSize = 11.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item.post.tags.take(4).forEach { tag ->
                AssistChip(onClick = {}, label = { Text("#$tag") })
            }
            if (item.linkedToTravelPath) {
                AssistChip(onClick = {}, label = { Text("TravelPath") }, leadingIcon = { Icon(Icons.Default.Route, null, modifier = Modifier.size(14.dp)) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onView, modifier = Modifier.weight(1f)) { Text("Voir") }
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Modifier") }
            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) { Text("Supprimer") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPublishedPostSheet(
    item: ProfilePostUi,
    onDismiss: () -> Unit,
    onSave: (ProfilePostUi) -> Unit
) {
    var title by remember(item.post.id) { mutableStateOf(item.title) }
    var description by remember(item.post.id) { mutableStateOf(item.description) }
    var visibility by remember(item.post.id) { mutableStateOf(item.visibility) }
    var tags by remember(item.post.id) { mutableStateOf(item.post.tags.joinToString(", ")) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Modifier la publication", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Stone800)
            SearchInput(value = title, placeholder = "Titre", onValueChange = { title = it })
            SearchInput(value = description, placeholder = "Description", onValueChange = { description = it })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Public", "Groupe").forEach { option ->
                    FilterChip(selected = visibility == option, onClick = { visibility = option }, label = { Text(option) })
                }
            }

            SearchInput(value = tags, placeholder = "Tags séparés par des virgules", onValueChange = { tags = it })

            Button(
                onClick = {
                    val parsedTags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(
                        item.copy(
                            title = title.ifBlank { item.title },
                            description = description,
                            visibility = visibility,
                            post = item.post.copy(tags = parsedTags.ifEmpty { item.post.tags }, description = description)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) { Text("Enregistrer") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SearchInput(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).background(CardBg, RoundedCornerShape(10.dp)).border(1.dp, StoneBorder, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = StoneMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = LocalTextStyle.current.copy(color = StoneText)
        ) { inner ->
            if (value.isBlank()) Text(placeholder, color = StoneMuted, fontSize = 13.sp)
            inner()
        }
    }
}
