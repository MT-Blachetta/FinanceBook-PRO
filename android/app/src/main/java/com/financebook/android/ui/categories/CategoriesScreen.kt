package com.financebook.android.ui.categories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.financebook.android.data.model.Category
import com.financebook.android.data.model.CategoryCreate
import com.financebook.android.data.model.CategoryType
import com.financebook.android.data.repository.FinanceRepository
import com.financebook.android.ui.components.ErrorText
import com.financebook.android.ui.components.LoadingBox
import com.financebook.android.ui.login.simpleFactory
import com.financebook.android.ui.theme.CardBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Category management: list per type, create (with optional parent),
 * delete (children are re-parented server-side, payments fall back to
 * UNCLASSIFIED).
 */
data class CategoriesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val categories: List<Category> = emptyList(),
    val types: List<CategoryType> = emptyList(),
    val newName: String = "",
    val newTypeId: Long? = null,
    val newParentId: Long? = null,
)

class CategoriesViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesUiState())
    val state: StateFlow<CategoriesUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val categories = repository.getCategories()
            val types = repository.getCategoryTypes()
            _state.value = _state.value.copy(
                isLoading = false,
                categories = categories.getOrDefault(emptyList()),
                types = types.getOrDefault(emptyList()),
                newTypeId = _state.value.newTypeId
                    ?: types.getOrDefault(emptyList()).firstOrNull()?.id,
                error = categories.exceptionOrNull()?.message
                    ?: types.exceptionOrNull()?.message,
            )
        }
    }

    fun update(transform: (CategoriesUiState) -> CategoriesUiState) {
        _state.value = transform(_state.value)
    }

    fun create() {
        val current = _state.value
        val typeId = current.newTypeId ?: return
        if (current.newName.isBlank()) return
        viewModelScope.launch {
            repository.createCategory(
                CategoryCreate(
                    name = current.newName.trim(),
                    typeId = typeId,
                    parentId = current.newParentId,
                ),
            ).onSuccess {
                _state.value = _state.value.copy(newName = "", newParentId = null)
                refresh()
            }.onFailure { throwable ->
                _state.value = _state.value.copy(error = throwable.message)
            }
        }
    }

    fun delete(categoryId: Long) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
                .onSuccess { refresh() }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(error = throwable.message)
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    repository: FinanceRepository,
    onBack: () -> Unit,
) {
    val viewModel: CategoriesViewModel =
        viewModel(factory = simpleFactory { CategoriesViewModel(repository) })
    val state by viewModel.state.collectAsState()
    var categoryPendingDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.error != null) ErrorText(state.error ?: "")

            // ── Create form ─────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("New category", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = state.newName,
                        onValueChange = { value -> viewModel.update { it.copy(newName = value) } },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeDropdown(
                            types = state.types,
                            selectedId = state.newTypeId,
                            onSelect = { id ->
                                viewModel.update { it.copy(newTypeId = id, newParentId = null) }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        ParentDropdown(
                            candidates = state.categories.filter { it.typeId == state.newTypeId },
                            selectedId = state.newParentId,
                            onSelect = { id -> viewModel.update { it.copy(newParentId = id) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Button(
                        onClick = { viewModel.create() },
                        enabled = state.newName.isNotBlank() && state.newTypeId != null,
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Add category") }
                }
            }

            when {
                state.isLoading -> LoadingBox()
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.categories, key = { it.id }) { category ->
                        val typeName = state.types
                            .firstOrNull { it.id == category.typeId }?.name ?: "?"
                        val parentName = state.categories
                            .firstOrNull { it.id == category.parentId }?.name

                        Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(category.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        buildString {
                                            append("Type: $typeName")
                                            if (parentName != null) append(" · Parent: $parentName")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (category.name != "UNCLASSIFIED") {
                                    IconButton(onClick = { categoryPendingDelete = category }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete ${category.name}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    categoryPendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryPendingDelete = null },
            title = { Text("Delete \"${category.name}\"?") },
            text = {
                Text(
                    "Child categories move up to the parent and payments in this " +
                        "category fall back to UNCLASSIFIED.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(category.id)
                    categoryPendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { categoryPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeDropdown(
    types: List<CategoryType>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = types.firstOrNull { it.id == selectedId }?.name ?: "Type"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onSelect(type.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentDropdown(
    candidates: List<Category>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = candidates.firstOrNull { it.id == selectedId }?.name ?: "No parent"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Parent") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("No parent") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            candidates.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
