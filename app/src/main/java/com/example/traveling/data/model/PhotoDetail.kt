package com.example.traveling.data.model

import androidx.compose.ui.graphics.Color

data class PhotoDetail(
    val id: String,
    val imageUrls: List<String>,
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
    val tags: List<String>
)