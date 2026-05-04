package com.example.traveling.data.model

import androidx.compose.ui.graphics.Color

// ─── 新增：单条评论的数据模型 ───
data class PhotoPostComment(
    val id: String,
    val author: String,
    val avatar: String,
    val color: Color,
    val text: String,
    val date: String,
    val likes: Int
)

// ─── 更新：照片详情的完整模型 ───
data class PhotoPostDetail(
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
    val howToGetThere: String,   // 交通指南
    val commentsList: List<PhotoPostComment> // 底部真实的评论列表
)