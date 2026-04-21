package com.example.traveling.features.travelshare
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.PhotoDetail
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── 页面状态密封类 ───
sealed class PhotoDetailUiState {
    object Loading : PhotoDetailUiState()
    data class Success(val photo: PhotoDetail) : PhotoDetailUiState()
    data class Error(val message: String) : PhotoDetailUiState()
}

class PhotoDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PhotoDetailUiState>(PhotoDetailUiState.Loading)
    val uiState: StateFlow<PhotoDetailUiState> = _uiState.asStateFlow()

    // 暴露一个方法供页面调用
    fun loadPhoto(photoId: String) {
        // 每次请求前重置为 Loading
        _uiState.value = PhotoDetailUiState.Loading

        viewModelScope.launch {
            try {
                // 模拟网络延迟 (真实场景下这里是 Repository 的挂起函数)
                delay(800)

                // 模拟根据 ID 去数据库查询数据
                val data = fetchPhotoFromDatabase(photoId)

                if (data != null) {
                    _uiState.value = PhotoDetailUiState.Success(data)
                } else {
                    _uiState.value = PhotoDetailUiState.Error("Photo introuvable.")
                }
            } catch (e: Exception) {
                _uiState.value = PhotoDetailUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    // ─── 模拟数据库查询 ───
    private fun fetchPhotoFromDatabase(id: String): PhotoDetail? {
        // 这里只是为了演示，真实项目中应该从 Room 或 Firebase 获取
        return PhotoDetail(
            id = id,
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1558507564-c573429b9ceb?w=800",
                "https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?w=800"
            ),
            location = "Grande Muraille", country = "Pékin, Chine", date = "15 mars 2026",
            author = "Li Xiaofang", authorAvatar = "L", authorColor = Color(0xFFB91C1C),
            likes = 1234, isLiked = true, isSaved = false,
            description = "La Grande Muraille s'étend sur des milliers de kilomètres, majestueuse. C'est une expérience inoubliable.",
            comments = 42, tags = listOf("Voyage", "Chine")
        )
    }
}