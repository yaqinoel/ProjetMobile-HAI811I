package com.example.traveling.features.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.features.travelshare.toPhotoPostUi
import com.example.traveling.ui.theme.CardBg
import com.example.traveling.ui.theme.PageBg
import com.example.traveling.ui.theme.RedPrimary
import com.example.traveling.ui.theme.Stone400
import com.example.traveling.ui.theme.Stone500
import com.example.traveling.ui.theme.Stone800
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface GroupDetailUiState {
    data object Loading : GroupDetailUiState
    data class Success(val posts: List<PhotoPostDocument>) : GroupDetailUiState
    data class Error(val message: String) : GroupDetailUiState
}

class GroupDetailViewModel(
    private val repository: PhotoPostRepository = PhotoPostRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null

    fun observe(groupId: String) {
        listener?.remove()
        _uiState.value = GroupDetailUiState.Loading
        listener = repository.observeGroupPublishedPosts(
            groupId = groupId,
            onChanged = { _uiState.value = GroupDetailUiState.Success(it) },
            onError = { _uiState.value = GroupDetailUiState.Error(it.localizedMessage ?: "Erreur de chargement") }
        )
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}

@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit = {},
    onOpenPhotoDetail: (String) -> Unit = {}
) {
    val viewModel: GroupDetailViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.observe(groupId)
    }

    Surface(color = PageBg, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Column {
                    Text("Groupe", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Stone800)
                    Text(groupId, fontSize = 11.sp, color = Stone500)
                }
            }

            when (val ui = state) {
                GroupDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RedPrimary)
                    }
                }
                is GroupDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(ui.message, color = Stone500)
                    }
                }
                is GroupDetailUiState.Success -> {
                    if (ui.posts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Aucune publication dans ce groupe.", color = Stone500)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(ui.posts, key = { it.postId }) { post ->
                                val item = post.toPhotoPostUi(isLiked = false, isSaved = false)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardBg, RoundedCornerShape(12.dp))
                                        .clickable { onOpenPhotoDetail(item.id) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val displayTitle = item.title.ifBlank { item.description.ifBlank { item.location } }
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.size(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayTitle,
                                            color = Stone800,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(item.location, color = Stone400, fontSize = 12.sp, maxLines = 1)
                                        Text("${item.likes} likes · ${item.comments} commentaires", color = Stone500, fontSize = 11.sp)
                                    }
                                    Icon(Icons.Default.Group, null, tint = RedPrimary)
                                }
                            }
                            item { Spacer(Modifier.height(12.dp)) }
                        }
                    }
                }
            }
        }
    }
}
