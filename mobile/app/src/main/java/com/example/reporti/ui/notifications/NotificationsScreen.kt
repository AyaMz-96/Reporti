package com.example.reporti.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reporti.data.NotificationDto
import com.example.reporti.ui.notificationColor
import com.example.reporti.ui.notificationIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(vm: NotificationViewModel) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { vm.markAllRead() }) {
                Text("Tout lire")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = state.unreadOnly,
                onClick = { vm.setUnreadOnly(!state.unreadOnly) },
                label = { Text("Non lues (${state.unreadCount})") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        state.error?.let { message ->
            Text(
                text = "Erreur : $message",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
            TextButton(onClick = { vm.refresh() }) {
                Text("Réessayer")
            }
        }

        if (state.loading && state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.items.isEmpty() && state.error == null) {
            Text(
                text = "Aucune notification",
                color = Color.Gray,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(state.items, key = { it.id }) { notification ->
                NotificationCard(notification) {
                    if (!notification.isRead) vm.markRead(notification.id)
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(n: NotificationDto, onClick: () -> Unit) {
    val background = if (n.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    }
    val typeStr = n.type ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = notificationColor(typeStr).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = notificationIcon(typeStr)),
                    contentDescription = typeLabel(n.type),
                    tint = notificationColor(typeStr),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = n.message,
                    fontSize = 15.sp,
                    fontWeight = if (n.isRead) FontWeight.Normal else FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${typeLabel(n.type)}  •  ${formatDate(n.createdAt)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun typeLabel(type: String?): String = when (type) {
    "STATUS_CHANGED" -> "Statut"
    "NEW_COMMENT" -> "Commentaire"
    "ASSIGNED" -> "Assignation"
    "PRIORITY_URGENT", "URGENT_UPDATE" -> "Urgent"
    "NEW_REPORT" -> "Nouveau signalement"
    "DISPUTE" -> "Litige"
    "AGENT_REFUSAL" -> "Refus agent"
    "SYSTEM_ALERT" -> "Système"
    else -> type ?: "Info"
}

private fun formatDate(iso: String?): String =
    iso?.take(16)?.replace('T', ' ') ?: ""
