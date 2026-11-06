package com.example.reporti.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
fun HomeScreen(vm: HomeViewModel) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.search() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Text("Signalements", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Rechercher (ex. fuite)") },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Rechercher",
                    modifier = Modifier.size(22.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search() })
        )

        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(STATUSES) { s ->
                FilterChip(
                    selected = state.status == s,
                    onClick = { vm.onStatusChange(if (state.status == s) null else s) },
                    label = { Text(s) }
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
                TextButton(onClick = { vm.reset() }) { Text("Réinitialiser") }
            }
        }

        Text("${state.total} résultat(s)", fontSize = 12.sp, color = Color.Gray)

        state.error?.let {
            Text("Erreur : $it", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        if (state.loading) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
        ) {
            items(state.results, key = { it.id }) { report ->
                val categoryName = state.categories[report.categoryId] ?: "Catégorie #${report.categoryId}"
                val sectorName = state.sectors[report.sectorId] ?: "Secteur #${report.sectorId}"
                ReportCard(report, categoryName, sectorName)
            }
        }
    }
}

@Composable
private fun ReportCard(r: ReportResponse, categoryName: String, sectorName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "#${r.id}  ${r.title.ifEmpty { r.description.take(90) }}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (r.title.isNotEmpty() && r.description.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    r.description.take(120),
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.status,
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
            Text("$categoryName • $sectorName", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
