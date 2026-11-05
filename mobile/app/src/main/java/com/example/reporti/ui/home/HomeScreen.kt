package com.example.reporti.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reporti.R
import com.example.reporti.data.ReportResponse
import com.example.reporti.ui.priorityColor
import com.example.reporti.ui.statusColor

private val STATUSES = listOf("RECEIVED", "IN_PROGRESS", "RESOLVED", "REJECTED")
private val PRIORITIES = listOf("URGENT", "NORMAL", "LOW")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    val state by vm.state.collectAsState()
    val results = state.filtered

    // Reload every time the tab is opened, so a report created in the form shows up (web does the same via ReportEvents)
    LaunchedEffect(Unit) { vm.load() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Text("Reports", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by title or description") },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Search",
                    modifier = Modifier.size(22.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )

        Spacer(Modifier.height(8.dp))

        // Category filter (the web list has it too)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.categories, key = { it.id }) { c ->
                FilterChip(
                    selected = state.categoryId == c.id,
                    onClick = { vm.onCategoryChange(if (state.categoryId == c.id) null else c.id) },
                    label = { Text(c.name) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(STATUSES) { s ->
                FilterChip(
                    selected = state.status == s,
                    onClick = { vm.onStatusChange(if (state.status == s) null else s) },
                    label = { Text(s.replace('_', ' ')) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PRIORITIES) { p ->
                FilterChip(
                    selected = state.priority == p,
                    onClick = { vm.onPriorityChange(if (state.priority == p) null else p) },
                    label = { Text(p) }
                )
            }
            item {
                TextButton(onClick = { vm.reset() }) { Text("Reset") }
            }
        }

        Text("${results.size} report(s)", fontSize = 12.sp, color = Color.Gray)

        state.error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            TextButton(onClick = { vm.load() }) { Text("Retry") }
        }

        if (state.loading && state.reports.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (results.isEmpty() && state.error == null) {
            Text("No reports match your search.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
        }

        val bottomPadding = if (bottomContentPadding > 0.dp) bottomContentPadding else 120.dp
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
        ) {
            items(results, key = { it.id }) { ReportCard(it, state.categoryName(it.categoryId)) }
        }
    }
}

@Composable
private fun ReportCard(r: ReportResponse, categoryName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(r.title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.status.replace('_', ' '),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor(r.status)
                )
                Text("  •  ", fontSize = 12.sp, color = Color.Gray)
                Text(
                    r.priority,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = priorityColor(r.priority)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text("$categoryName • ${shortDate(r.createdAt)}", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

private fun shortDate(iso: String?): String =
    iso?.take(10) ?: ""
