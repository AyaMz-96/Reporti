package com.example.reporti.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reporti.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUsersUiState(
    val users: List<AdminUser> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class AdminUsersViewModel : ViewModel() {
    private val _state = MutableStateFlow(AdminUsersUiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val users = AdminRepository.listUsers()
                _state.update { it.copy(users = users, loading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }

    fun createUser(body: CreateAdminUserBody) = runOp {
        AdminRepository.createUser(body)
        _state.update { it.copy(message = "User created.") }
        load()
    }

    fun updateUser(id: String, body: UpdateAdminUserBody) = runOp {
        AdminRepository.updateUser(id, body)
        _state.update { it.copy(message = "User updated.") }
        load()
    }

    fun deleteUser(id: String) = runOp {
        AdminRepository.deleteUser(id)
        _state.update { it.copy(message = "User deleted.") }
        load()
    }

    private fun runOp(op: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, message = null) }
            try { op() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(error = e.toUserMessage()) } }
            finally { _state.update { it.copy(saving = false) } }
        }
    }
}

/** Mode is determined by `filterRole`: null shows all, "agent" filters to agents. */
@Composable
fun AdminUsersScreen(
    title: String,
    filterRole: String?,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    vm: AdminUsersViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var formOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AdminUser?>(null) }
    var confirmDelete by remember { mutableStateOf<AdminUser?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    val visible = remember(state.users, filterRole) {
        if (filterRole == null) state.users else state.users.filter { it.role == filterRole }
    }

    Column(Modifier.fillMaxSize()) {
        AdminTopBar(title, onBack = onBack)
        Box(Modifier.fillMaxSize().weight(1f)) {
            if (formOpen) {
                UserForm(
                    initial = editing,
                    defaultRole = filterRole ?: "citizen",
                    saving = state.saving,
                    onCancel = { formOpen = false; editing = null },
                    onCreate = { vm.createUser(it); formOpen = false; editing = null },
                    onUpdate = { id, body -> vm.updateUser(id, body); formOpen = false; editing = null },
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${visible.size} result(s)", color = Color.Gray, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { editing = null; formOpen = true }) {
                            Text(if (filterRole == "agent") "+ New agent" else "+ New user")
                        }
                    }
                    state.error?.let { AdminBanner(it, isError = true) }
                    state.message?.let { AdminBanner(it, isError = false) }

                    if (state.loading && visible.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, bottomContentPadding),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(visible, key = { it.id }) { user ->
                                UserRow(
                                    user = user,
                                    onEdit = { editing = user; formOpen = true },
                                    onToggleSuspend = {
                                        vm.updateUser(user.id, UpdateAdminUserBody(suspended = !user.suspended))
                                    },
                                    onDelete = { confirmDelete = user },
                                )
                            }
                            if (visible.isEmpty()) {
                                item { Text("No results.", color = Color.Gray) }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${user.email}?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                Button(onClick = { vm.deleteUser(user.id); confirmDelete = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun UserRow(
    user: AdminUser,
    onEdit: () -> Unit,
    onToggleSuspend: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.fullName.ifBlank { user.email }, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Badge(user.role)
                Spacer(Modifier.width(6.dp))
                Badge(if (user.suspended) "suspended" else "active", danger = user.suspended)
            }
            if (user.fullName.isNotBlank() && user.fullName != user.email) {
                Text(user.email, fontSize = 12.sp, color = Color.Gray)
            }
            user.phone?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
            if (user.role == "agent") {
                Text(
                    "Categories: ${user.categoryIds.joinToString(", ").ifBlank { "—" }} · Sector: ${user.sectorId ?: "any"}",
                    fontSize = 12.sp, color = Color.Gray,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onToggleSuspend) { Text(if (user.suspended) "Activate" else "Suspend") }
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun Badge(text: String, danger: Boolean = false) {
    Surface(
        color = if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp)
    }
}

@Composable
private fun UserForm(
    initial: AdminUser?,
    defaultRole: String,
    saving: Boolean,
    onCancel: () -> Unit,
    onCreate: (CreateAdminUserBody) -> Unit,
    onUpdate: (String, UpdateAdminUserBody) -> Unit,
) {
    var fullName by remember { mutableStateOf(initial?.fullName ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(initial?.role ?: defaultRole) }
    var categoryIds by remember { mutableStateOf(initial?.categoryIds ?: emptyList<Long>()) }
    var sectorId by remember { mutableStateOf(initial?.sectorId) }
    var localError by remember { mutableStateOf<String?>(null) }

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var sectors by remember { mutableStateOf<List<Sector>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            categories = ReportRepository.getCategories()
            sectors = ReportRepository.getSectors()
        } catch (_: Exception) { }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (initial == null) "New ${if (defaultRole == "agent") "agent" else "user"}" else "Edit user",
            fontSize = 20.sp, fontWeight = FontWeight.Bold,
        )

        AdminField(fullName, { fullName = it }, "Full name")
        AdminField(email, { email = it }, "Email")
        if (initial == null) {
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password (min 6)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
        }
        AdminField(phone, { phone = it }, "Phone (optional)")

        if (defaultRole != "agent") {
            Text("Role", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            RolePicker(current = role, onSelect = { role = it })
        }

        if (role == "agent") {
            Text("Categories", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            CategoryPicker(
                all = categories, selected = categoryIds,
                onToggle = { id -> categoryIds = if (id in categoryIds) categoryIds - id else categoryIds + id },
            )
            Text("Sector", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            SectorPicker(
                all = sectors, selected = sectorId,
                onSelect = { sectorId = it },
            )
        }

        localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !saving,
                onClick = {
                    localError = null
                    if (fullName.isBlank()) { localError = "Full name is required."; return@Button }
                    if (initial == null && email.isBlank()) { localError = "Email is required."; return@Button }
                    if (initial == null && password.length < 6) { localError = "Password must have at least 6 characters."; return@Button }

                    val cats = if (role == "agent") categoryIds else null
                    val sec = if (role == "agent") sectorId else null

                    if (initial == null) {
                        onCreate(
                            CreateAdminUserBody(
                                email = email.trim(),
                                password = password,
                                fullName = fullName.trim(),
                                phone = phone.trim().ifBlank { null },
                                role = role,
                                categoryIds = cats,
                                sectorId = sec,
                            ),
                        )
                    } else {
                        onUpdate(
                            initial.id,
                            UpdateAdminUserBody(
                                fullName = fullName.trim(),
                                phone = phone.trim().ifBlank { null },
                                role = role,
                                categoryIds = cats,
                                sectorId = sec,
                            ),
                        )
                    }
                },
            ) { Text(if (saving) "Saving…" else "Save") }
            OutlinedButton(onClick = onCancel, enabled = !saving) { Text("Cancel") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RolePicker(current: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("citizen", "agent", "sector_manager", "admin").forEach { r ->
            FilterChip(selected = current == r, onClick = { onSelect(r) }, label = { Text(r) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(all: List<Category>, selected: List<Long>, onToggle: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        all.forEach { c ->
            FilterChip(selected = c.id in selected, onClick = { onToggle(c.id) }, label = { Text(c.name) })
        }
        if (all.isEmpty()) Text("No categories exist yet.", fontSize = 12.sp, color = Color.Gray)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectorPicker(all: List<Sector>, selected: Long?, onSelect: (Long?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Any") })
        all.forEach { s ->
            FilterChip(selected = selected == s.id, onClick = { onSelect(s.id) }, label = { Text(s.name) })
        }
    }
}
