package com.example.reporti.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.reporti.data.AuthViewModel
import com.example.reporti.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminDashboardViewModel : ViewModel() {
    data class State(
        val total: Int = 0,
        val agents: Int = 0,
        val suspended: Int = 0,
        val error: String? = null,
    )
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            try {
                val users = AdminRepository.listUsers()
                _state.update {
                    it.copy(
                        total = users.size,
                        agents = users.count { u -> u.role == "agent" },
                        suspended = users.count { it.suspended },
                    )
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(
    bottomContentPadding: Dp,
    authVm: AuthViewModel,
    onNavigate: (AdminPage) -> Unit,
    vm: AdminDashboardViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    val user = authVm.state.user

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Admin", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        user?.let { Text("Signed in as ${it.fullName}", color = Color.Gray, fontSize = 13.sp) }

        state.error?.let { AdminBanner(it, isError = true) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Users", state.total, Modifier.weight(1f))
            StatTile("Agents", state.agents, Modifier.weight(1f))
            StatTile("Suspended", state.suspended, Modifier.weight(1f))
        }

        HorizontalDivider()

        Text("Manage", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        NavTile("Users", "All accounts — create, edit, suspend", onClick = { onNavigate(AdminPage.Users) })
        NavTile("Agents", "Agents and their assignment categories", onClick = { onNavigate(AdminPage.Agents) })
        NavTile("Categories", "Categories used when filing a report", onClick = { onNavigate(AdminPage.Categories) })
        NavTile("Assignments", "Dispatch received reports to agents", onClick = { onNavigate(AdminPage.Assignments) })

        Spacer(Modifier.height(bottomContentPadding))
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun NavTile(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
