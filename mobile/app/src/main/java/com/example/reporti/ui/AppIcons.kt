package com.example.reporti.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.reporti.R

fun statusColor(status: String): Color = when (status) {
    "IN_PROGRESS" -> Color(0xFFF59E0B)
    "RESOLVED" -> Color(0xFF2E7D32)
    "REJECTED" -> Color(0xFFC62828)
    else -> Color(0xFF607D8B)   // RECEIVED
}

fun priorityColor(priority: String): Color = when (priority) {
    "URGENT" -> Color(0xFFC62828)
    "NORMAL" -> Color(0xFF1565C0)
    else -> Color(0xFF9E9E9E)   // LOW
}

// Uniquement tes 6 icônes existantes
@DrawableRes
fun notificationIcon(type: String): Int = when (type) {
    "NEW_COMMENT" -> R.drawable.ic_edit
    "ASSIGNED" -> R.drawable.ic_person
    "NEW_REPORT" -> R.drawable.ic_map
    else -> R.drawable.ic_notifications   // statut, urgent, système
}

fun notificationColor(type: String): Color = when (type) {
    "PRIORITY_URGENT" -> Color(0xFFC62828)
    "STATUS_CHANGED" -> Color(0xFFF59E0B)
    "NEW_COMMENT" -> Color(0xFF1565C0)
    "ASSIGNED" -> Color(0xFF6A1B9A)
    "NEW_REPORT" -> Color(0xFF2E7D32)
    else -> Color(0xFF607D8B)
}