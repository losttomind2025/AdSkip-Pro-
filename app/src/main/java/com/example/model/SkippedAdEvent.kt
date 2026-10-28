package com.example.model

data class SkippedAdEvent(
    val id: String,
    val appName: String,
    val packageName: String,
    val timestamp: Long,
    val timeSavedSec: Int = 10,
    val actionType: String = "AUTO_SKIPPED"
)
