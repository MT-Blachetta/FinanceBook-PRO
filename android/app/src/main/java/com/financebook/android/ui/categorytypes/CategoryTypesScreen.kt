package com.financebook.android.ui.categorytypes

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
import com.financebook.android.data.model.CategoryType
import com.financebook.android.data.model.CategoryTypeCreate
import com.financebook.android.data.repository.FinanceRepository
import com.financebook.android.ui.components.ErrorText
import com.financebook.android.ui.components.LoadingBox
import com.financebook.android.ui.login.simpleFactory
import com.financebook.android.ui.theme.CardBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Category type management: list, create, delete (guarded server-side). */
data class CategoryTypesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val types: List<CategoryType> = emptyList(),
    val newName: String = "",
    val newDescription: String = "",
)

class CategoryTypesViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(CategoryTypesUiState())
    val state: StateFlow<CategoryTypesUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.getCategoryTypes()
                .onSuccess { types ->
                    _state.value = _state.value.copy(isLoading = false, types = types)
                }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(isLoading = false, error = throwable.message)
                }
        }
    }

    fun update(transform: (CategoryTypesUiState) -> CategoryTypesUiState) {
        _state.value = transform(_state.value)
    }

    fun create() {
        val current = _state.value
        if (current.newName.isBlank()) return
        viewModelScope.launch {
            repository.createCategoryType(
                CategoryTypeCreate(
                    name = current.newName.trim(),
                    description = current.newDescription.ifBlank { null },
                ),
            ).onSuccess {
                _state.value = _state.value.copy(newName = "", newDescription = "")
                refresh()
            }.onFailure { throwable ->
                _state.value = _state.value.copy(error = throwable.message)
            }
        }
    }

    fun delete(typeId: Long) {
        viewModelScope.launch {
            repository.deleteCategoryType(typeId)
                .onSuccess { refresh() }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(error = throwable.message)
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTypesScreen(
    repository: FinanceRepository,
    onBack: () -> Unit,
) {
    val viewModel: CategoryTypesViewModel =
        viewModel(factory = simpleFactory { CategoryTypesViewModel(repository) })
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Category Types") },
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
                    Text("New category type", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = state.newName,
                        onValueChange = { value -> viewModel.update { it.copy(newName = value) } },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.newDescription,
                        onValueChange = { value ->
                            viewModel.update { it.copy(newDescription = value) }
                        },
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { viewModel.create() },
                        enabled = state.newName.isNotBlank(),
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Add type") }
                }
            }

            when {
                state.isLoading -> LoadingBox()
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.types, key = { it.id }) { type ->
                        Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(type.name, style = MaterialTheme.typography.titleSmall)
                                    if (!type.description.isNullOrBlank()) {
                                        Text(
                                            type.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (type.name != "standard") {
                                    IconButton(onClick = { viewModel.delete(type.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete ${type.name}",
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
}
