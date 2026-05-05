package com.example.traveling.data.model

import com.google.firebase.Timestamp

/** reports/{reportId} */
data class ReportDocument(
    val reportId: String = "",
    val postId: String = "",
    val reporterId: String? = null,
    val reason: String = "",
    val description: String? = null,
    val createdAt: Timestamp? = null,
    val status: String = "pending"
)
