package com.example.reporti.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reporti.data.AdminRepository
import com.example.reporti.data.AdminUser
import com.example.reporti.data.ApiClient
import com.example.reporti.data.ReportResponse
import com.example.reporti.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminAssignmentsUiState(
    val reports: List<ReportResponse> = emptyList(),
    val availableFor: ReportResponse? = null,
    val availableAgents: List<AdminUser> = emptyList(),
    val loading: Boolean = false,
    val agentsLoading: Boolean = false,
    val assigning: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class AdminAssignmentsViewModel : ViewModel() {
    private val _state = MutableStateFlow(AdminAssignmentsUiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val all = ApiClient.api.getReports()
                val received = all.filter { it.status == "RECEIVED" }
                _state.update { it.copy(reports = received, loading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }

    fun openAssign(report: ReportResponse) {
        _state.update {
            it.copy(availableFor = report, availableAgents = emptyList(), agentsLoading = true, error = null, message = null)
        }
        viewModelScope.launch {
            try {
                val agents = AdminRepository.getAvailableAgents(report.categoryId, report.sectorId)
                _state.update { it.copy(availableAgents = agents, agentsLoading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(agentsLoading = false, error = e.toUserMessage()) } }
        }
    }

    fun closeAssign() {
        _state.update { it.copy(availableFor = null, availableAgents = emptyList()) }
    }

    fun assign(agentId: String) {
        val report = _state.value.availableFor ?: return
        viewModelScope.launch {
            _state.update { it.copy(assigning = true, error = null) }
            try {
                AdminRepository.assignReport(report.id, agentId)
                _state.update {
                    it.copy(
                        assigning = false,
                        availableFor = null,
                        availableAgents = emptyList(),
                        message = "Report #${report.id} assigned. Status is now IN PROGRESS.",
                    )
                }
                load()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(assigning = false, error = e.toUserMessage()) } }
        }
    }
}

@Composable
fun AdminAssignmentsScreen(
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    vm: AdminAssignmentsViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize()) {
        AdminTopBar("Assignments", onBack = onBack)

        state.error?.let { AdminBanner(it, isError = true) }
        state.message?.let { AdminBanner(it, isError = false) }

        if (state.loading && state.reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.reports.isEmpty()) {
            Text("No reports waiting for assignment.", Modifier.padding(24.dp), color = Color.Gray)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.reports, key = { it.id }) { report ->
                    ReportAssignRow(report, onAssign = { vm.openAssign(report) })
                }
            }
        }
    }

    state.availableFor?.let { report ->
        AlertDialog(
            onDismissRequest = { if (!state.assigning) vm.closeAssign() },
            title = { Text("Assign report #${report.id}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(report.title, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Priority: ${report.priority} · Category #${report.categoryId} · Sector #${report.sectorId}",
                        fontSize = 12.sp, color = Color.Gray,
                    )
                    Spacer(Modifier.height(12.dp))
                    when {
                        state.agentsLoading -> Text("Loading available agents…")
                        state.availableAgents.isEmpty() ->
                            Text("No available agents match this report's category and sector.")
                        else -> {
                            Text("${state.availableAgents.size} agent(s) available:", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            state.availableAgents.forEach { agent ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            agent.fullName.ifBlank { agent.email },
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                        )
                                        Text(agent.email, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Button(
                                        enabled = !state.assigning,
                                        onClick = { vm.assign(agent.id) },
                                    ) { Text("Assign") }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.closeAssign() }, enabled = !state.assigning) { Text("Close") }
            },
        )
    }
}

@Composable
private fun ReportAssignRow(report: ReportResponse, onAssign: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("#${report.id} · ${report.title}", fontWeight = FontWeight.Medium)
            Text(
                "${report.priority} · ${report.createdAt?.take(10) ?: ""}",
                fontSize = 12.sp, color = Color.Gray,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAssign, modifier = Modifier.fillMaxWidth()) { Text("Assign an agent") }
        }
    }
}
