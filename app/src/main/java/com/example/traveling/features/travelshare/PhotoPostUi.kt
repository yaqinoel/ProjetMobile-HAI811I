package com.example.traveling.features.travelshare

import androidx.compose.ui.graphics.Color
import com.example.traveling.data.model.PhotoCommentDocument
import com.example.traveling.data.model.PhotoPostDocument
import java.text.SimpleDateFormat
import java.util.Locale

data class PhotoPostUi(
    val id: String,
    val imageUrl: String,
    val location: String,
    val country: String,
    val date: String,
    val author: String,
    val authorAvatar: String,
    val authorColor: Color,
    val likes: Int,
    val isLiked: Boolean,
    val isSaved: Boolean,
    val description: String,
    val comments: Int,
    val tags: List<String>,
    val placeType: String,
    val period: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationPrecision: String = "exact",
    val title: String = "",
    val city: String = "",
    val createdAtMillis: Long? = null,
    val visibility: String = "public",
    val groupName: String? = null,
    val authorId: String = "",
    val authorAvatarUrl: String? = null
)

data class PhotoPostCommentUi(
    val id: String,
    val author: String,
    val avatar: String,
    val color: Color,
    val text: String,
    val date: String,
    val likes: Int,
    val avatarUrl: String? = null
)

data class PhotoPostDetailUi(
    val id: String,
    val imageUrls: List<String>,
    val location: String,
    val country: String,
    val lat: Double,             // 纬度 (用于渲染经纬度数字)
    val lng: Double,             // 新增：经度 (用于生成地图路线)
    val date: String,
    val author: String,
    val authorAvatar: String,
    val authorColor: Color,
    val likes: Int,
    val isLiked: Boolean,
    val isSaved: Boolean,
    val description: String,
    val commentsCount: Int,
    val tags: List<String>,
    val howToGetThere: String,
    val commentsList: List<PhotoPostCommentUi>,
    val locationPrecision: String,
    val voiceNoteUrl: String? = null,
    val title: String = "",
    val authorId: String = "",
    val authorAvatarUrl: String? = null,
    val city: String = "",
    val placeType: String = ""
)

fun PhotoPostDocument.toPhotoPostUi(
    isLiked: Boolean = false,
    isSaved: Boolean = false
): PhotoPostUi {
    val visibleLat = displayLatitude ?: latitude ?: rawLatitude
    val visibleLng = displayLongitude ?: longitude ?: rawLongitude
    return PhotoPostUi(
        id = postId,
        imageUrl = imageUrls.firstOrNull().orEmpty(),
        location = locationName.ifBlank { city ?: country ?: "Lieu inconnu" },
        country = listOfNotNull(city, country).joinToString(", ").ifBlank { country.orEmpty() },
        date = createdAt?.toDate()?.let { DISPLAY_DATE_FORMAT.format(it) } ?: periodLabel.orEmpty(),
        author = authorName.ifBlank { "Voyageur" },
        authorAvatar = authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
        authorAvatarUrl = authorAvatarUrl,
        authorColor = avatarColorFor(authorId.ifBlank { authorName }),
        likes = likeCount,
        isLiked = isLiked,
        isSaved = isSaved,
        description = description.ifBlank { title },
        comments = commentCount,
        tags = tags,
        placeType = placeType,
        period = periodLabel ?: "all",
        latitude = visibleLat,
        longitude = visibleLng,
        locationPrecision = locationPrecision,
        title = title,
        city = city.orEmpty(),
        createdAtMillis = createdAt?.toDate()?.time,
        visibility = visibility,
        groupName = groupName,
        authorId = authorId
    )
}

fun PhotoPostDocument.toPhotoPostDetailUi(
    comments: List<PhotoCommentDocument> = emptyList(),
    isLiked: Boolean = false,
    isSaved: Boolean = false
): PhotoPostDetailUi {
    val visibleLat = displayLatitude ?: latitude ?: rawLatitude
    val visibleLng = displayLongitude ?: longitude ?: rawLongitude
    return PhotoPostDetailUi(
        id = postId,
        imageUrls = imageUrls,
        location = locationName.ifBlank { city ?: country ?: "Lieu inconnu" },
        country = listOfNotNull(city, country).joinToString(", ").ifBlank { country.orEmpty() },
        lat = visibleLat ?: 0.0,
        lng = visibleLng ?: 0.0,
        date = createdAt?.toDate()?.let { DISPLAY_DATE_FORMAT.format(it) } ?: periodLabel.orEmpty(),
        author = authorName.ifBlank { "Voyageur" },
        authorAvatar = authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
        authorAvatarUrl = authorAvatarUrl,
        authorColor = avatarColorFor(authorId.ifBlank { authorName }),
        likes = likeCount,
        isLiked = isLiked,
        isSaved = isSaved,
        description = description.ifBlank { title },
        commentsCount = commentCount,
        tags = tags,
        howToGetThere = "Ouvrir l'itinéraire avec Google Maps depuis cette fiche.",
        voiceNoteUrl = voiceNoteUrl,
        commentsList = comments.map { it.toPhotoPostCommentUi() },
        locationPrecision = locationPrecision,
        title = title,
        authorId = authorId,
        city = city.orEmpty(),
        placeType = placeType
    )
}

fun PhotoCommentDocument.toPhotoPostCommentUi(): PhotoPostCommentUi {
    return PhotoPostCommentUi(
        id = commentId,
        author = authorName.ifBlank { "Voyageur" },
        avatar = authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
        avatarUrl = authorAvatarUrl,
        color = avatarColorFor(authorId.ifBlank { authorName }),
        text = content,
        date = createdAt?.toDate()?.let { DISPLAY_DATE_FORMAT.format(it) } ?: "",
        likes = 0
    )
}

private val DISPLAY_DATE_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.FRANCE)

private fun avatarColorFor(seed: String): Color {
    val palette = listOf(
        Color(0xFFB91C1C),
        Color(0xFFD97706),
        Color(0xFF7C3AED),
        Color(0xFF0D9488),
        Color(0xFF2563EB),
        Color(0xFFDC2626)
    )
    val index = seed.fold(0) { acc, char -> acc + char.code }.mod(palette.size)
    return palette[index]
}
