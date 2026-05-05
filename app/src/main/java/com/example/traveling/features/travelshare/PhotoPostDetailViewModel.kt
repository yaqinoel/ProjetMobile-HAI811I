package com.example.traveling.features.travelshare

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.features.travelshare.model.PhotoPostCommentUi
import com.example.traveling.features.travelshare.model.PhotoPostDetailUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── 页面状态密封类 ───
sealed class PhotoPostDetailUiState {
    object Loading : PhotoPostDetailUiState()
    data class Success(val photo: PhotoPostDetailUi) : PhotoPostDetailUiState()
    data class Error(val message: String) : PhotoPostDetailUiState()
}

class PhotoPostDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PhotoPostDetailUiState>(PhotoPostDetailUiState.Loading)
    val uiState: StateFlow<PhotoPostDetailUiState> = _uiState.asStateFlow()

    // 暴露一个方法供页面调用
    fun loadPhoto(photoId: String) {
        // 每次请求前重置为 Loading
        _uiState.value = PhotoPostDetailUiState.Loading

        viewModelScope.launch {
            try {
                // 模拟网络延迟 (真实场景下这里是 Repository 的挂起函数)
                delay(400)

                // 模拟根据 ID 去数据库查询数据
                val data = fetchPhotoFromDatabase(photoId)

                if (data != null) {
                    _uiState.value = PhotoPostDetailUiState.Success(data)
                } else {
                    _uiState.value = PhotoPostDetailUiState.Error("Photo introuvable.")
                }
            } catch (e: Exception) {
                _uiState.value = PhotoPostDetailUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    // ─── 模拟数据库查询 (已更新 Figma 完整数据) ───
    private fun fetchPhotoFromDatabase(id: String): PhotoPostDetailUi? {
        // 构建模拟的评论列表
        val mockComments = listOf(
            PhotoPostCommentUi("c1", "Zhang Zhiyuan", "Z", Color(0xFF7C3AED), "Magnifique ! La lumière est parfaite, comme une peinture.", "Il y a 2 heures", 15),
            PhotoPostCommentUi("c2", "Wang Wanqing", "W", Color(0xFFD97706), "J'y suis allé le mois dernier, c'est encore plus impressionnant en vrai !", "Il y a 5 heures", 32),
            PhotoPostCommentUi("c3", "Chen Minghui", "C", Color(0xFF0D9488), "Quel objectif avez-vous utilisé ?", "Il y a 1 jour", 4)
        )

        // 返回完整的 PhotoDetail 对象
        return PhotoPostDetailUi(
            id = id,
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1558507564-c573429b9ceb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800",
                "https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800"
            ),
            location = "Grande Muraille",
            country = "Pékin, Chine",
            lat = 40.4319,
            lng = 116.5704,
            date = "15 mars 2026",
            author = "Li Xiaofang",
            authorAvatar = "L",
            authorColor = Color(0xFFB91C1C),
            likes = 1234,
            isLiked = true,
            isSaved = false,
            description = "La Grande Muraille s'étend sur des milliers de kilomètres, majestueuse et spectaculaire. À l'aube, la lumière dorée inonde les remparts, un spectacle à couper le souffle.",
            commentsCount = 3,
            tags = listOf("Grande Muraille", "Lever du soleil", "Monument", "Pékin"),
            howToGetThere = "Métro ligne 2 jusqu'à Jishuitan, puis bus 877 direct vers la Grande Muraille de Badaling. Ou train intercité S2 depuis la gare de Pékin Nord.", // 👈 新增
            commentsList = mockComments
        )
    }
}