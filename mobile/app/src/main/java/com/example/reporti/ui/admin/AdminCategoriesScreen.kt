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
import com.example.reporti.data.Category
import com.example.reporti.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminCategoriesUiState(
    val categories: List<Category> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class AdminCategoriesViewModel : ViewModel() {
    private val _state = MutableStateFlow(AdminCategoriesUiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                // Read via ReportRepository to reuse existing endpoint
                val list = com.example.reporti.data.ReportRepository.getCategories()
                _state.update { it.copy(categories = list, loading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }

    fun create(name: String, description: String?) = runOp {
        AdminRepository.createCategory(name, description)
        _state.update { it.copy(message = "Category created.") }
        load()
    }

    fun update(id: Long, name: String, description: String?) = runOp {
        AdminRepository.updateCategory(id, name, description)
        _state.update { it.copy(message = "Category updated.") }
        load()
    }

    fun delete(id: Long) = runOp {
        AdminRepository.deleteCategory(id)
        _state.update { it.copy(message = "Category deleted.") }
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

@Composable
fun AdminCategoriesScreen(
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    vm: AdminCategoriesViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var formOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var confirmDelete by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize()) {
        AdminTopBar("Categories", onBack = onBack)
        Box(Modifier.fillMaxSize().weight(1f)) {
            if (formOpen) {
                CategoryForm(
                    initial = editing,
                    saving = state.saving,
                    onCancel = { formOpen = false; editing = null },
                    onSubmit = { name, desc ->
                        if (editing == null) vm.create(name, desc) else vm.update(editing!!.id, name, desc)
                        formOpen = false; editing = null
                    },
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${state.categories.size} category(ies)", color = Color.Gray, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { editing = null; formOpen = true }) { Text("+ New category") }
                    }
                    state.error?.let { AdminBanner(it, isError = true) }
                    state.message?.let { AdminBanner(it, isError = false) }

                    if (state.loading && state.categories.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, bottomContentPadding),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.categories, key = { it.id }) { cat ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    tonalElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("#${cat.id} · ${cat.name}", fontWeight = FontWeight.Medium)
                                        Text(cat.description ?: "—", fontSize = 12.sp, color = Color.Gray)
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = { editing = cat; formOpen = true }) { Text("Edit") }
                                            TextButton(onClick = { confirmDelete = cat }) {
                                                Text("Delete", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                            if (state.categories.isEmpty()) {
                                item { Text("No categories yet.", color = Color.Gray) }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${cat.name}?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                Button(onClick = { vm.delete(cat.id); confirmDelete = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CategoryForm(
    initial: Category?,
    saving: Boolean,
    onCancel: () -> Unit,
    onSubmit: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(if (initial == null) "New category" else "Edit category", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        AdminField(name, { name = it }, "Name")
        AdminField(description, { description = it }, "Description (optional)", singleLine = false)
        localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !saving,
                onClick = {
                    localError = null
                    if (name.isBlank()) { localError = "Name is required."; return@Button }
                    onSubmit(name.trim(), description.trim().ifBlank { null })
                },
            ) { Text(if (saving) "Saving…" else "Save") }
            OutlinedButton(onClick = onCancel, enabled = !saving) { Text("Cancel") }
        }
    }
}
