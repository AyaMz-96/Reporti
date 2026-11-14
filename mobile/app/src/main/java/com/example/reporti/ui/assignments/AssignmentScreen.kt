package com.example.reporti.ui.assignments

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reporti.data.ReportResponse
import com.example.reporti.ui.priorityColor
import com.example.reporti.ui.statusColor

@Composable
fun AssignmentScreen(
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    vm: AssignmentViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var pendingTake by remember { mutableStateOf<ReportResponse?>(null) }

    // Reload on open, and whenever the tab is revisited
    LaunchedEffect(Unit) { vm.load() }

    val bottomPadding = if (bottomContentPadding > 0.dp) bottomContentPadding else 120.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Available for you", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Reports you can take up right now",
                fontSize = 13.sp,
                color = Color.Gray,
            )
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.availableLoading -> item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.available.isEmpty() -> item {
                Text(
                    "No new reports match your profile right now. If you haven't set your categories and sector yet, do so in your Profile.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                )
            }
            else -> items(state.available, key = { "av-${it.id}" }) { report ->
                AvailableReportCard(report, onTake = { pendingTake = report })
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text("Your active tasks", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Reports you have already taken up",
                fontSize = 13.sp,
                color = Color.Gray,
            )
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.activeLoading -> item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.active.isEmpty() -> item {
                Text(
                    "You haven't taken any tasks yet.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                )
            }
            else -> items(state.active, key = { "ac-${it.id}" }) { report ->
                ActiveReportCard(report)
            }
        }

        state.error?.let { msg ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        state.message?.let { msg ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }

    // Confirmation dialog: agent self-service claim
    pendingTake?.let { report ->
        AlertDialog(
            onDismissRequest = { if (!state.claiming) pendingTake = null },
            title = { Text("Take this task?") },
            text = {
                Column {
                    Text("#${report.id} · ${report.title}", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This report will be assigned to you and its status will become IN PROGRESS.",
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !state.claiming,
                    onClick = {
                        vm.claim(report.id)
                        pendingTake = null
                    },
                ) {
                    Text(if (state.claiming) "Taking…" else "Yes, take it")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.claiming,
                    onClick = { pendingTake = null },
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AvailableReportCard(report: ReportResponse, onTake: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${report.id}", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.size(8.dp))
                Text(
                    report.priority,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = priorityColor(report.priority),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(report.title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${report.status.replace('_', ' ')} · ${shortDate(report.createdAt)}",
                fontSize = 12.sp,
                color = Color.Gray,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onTake, modifier = Modifier.fillMaxWidth()) {
                Text("Take this task")
            }
        }
    }
}

@Composable
private fun ActiveReportCard(report: ReportResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${report.id}", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.size(8.dp))
                Text(
                    report.status.replace('_', ' '),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor(report.status),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    report.priority,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = priorityColor(report.priority),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(report.title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(shortDate(report.createdAt), fontSize = 12.sp, color = Color.Gray)
        }
    }
}

private fun shortDate(iso: String?): String = iso?.take(10) ?: ""
