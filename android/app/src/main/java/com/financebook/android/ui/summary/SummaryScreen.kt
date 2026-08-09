package com.financebook.android.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.financebook.android.data.model.Category
import com.financebook.android.data.model.PaymentItem
import com.financebook.android.data.repository.FinanceRepository
import com.financebook.android.ui.components.AmountText
import com.financebook.android.ui.components.ErrorText
import com.financebook.android.ui.components.LoadingBox
import com.financebook.android.ui.components.formatAmount
import com.financebook.android.ui.login.simpleFactory
import com.financebook.android.ui.theme.CardBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** View filter, mirroring the web app: All / Expenses / Incomes / Fees. */
enum class ViewFilter(val label: String) {
    ALL("All"),
    EXPENSES("Expenses"),
    INCOMES("Incomes"),
    FEES("Fees"),
}

data class SummaryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val items: List<PaymentItem> = emptyList(),
    val filter: ViewFilter = ViewFilter.ALL,
    val categories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<Long> = emptySet(),
) {
    /** Items after the fee filter (server already applied direction/category). */
    val visibleItems: List<PaymentItem>
        get() = if (filter == ViewFilter.FEES) {
            items.filter { (it.transactionFee ?: 0.0) >= 0.01 }
        } else {
            items
        }

    val total: Double
        get() = if (filter == ViewFilter.FEES) {
            visibleItems.sumOf { it.transactionFee ?: 0.0 }
        } else {
            visibleItems.sumOf { it.amount }
        }
}

class SummaryViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(SummaryUiState())
    val state: StateFlow<SummaryUiState> = _state

    init {
        refresh()
        loadCategories()
    }

    fun setFilter(filter: ViewFilter) {
        _state.value = _state.value.copy(filter = filter)
        refresh()
    }

    fun toggleCategory(categoryId: Long) {
        val current = _state.value.selectedCategoryIds
        val updated = if (categoryId in current) current - categoryId else current + categoryId
        _state.value = _state.value.copy(selectedCategoryIds = updated)
        refresh()
    }

    fun clearCategoryFilter() {
        _state.value = _state.value.copy(selectedCategoryIds = emptySet())
        refresh()
    }

    fun refresh() {
        val snapshot = _state.value
        _state.value = snapshot.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.getPaymentItems(
                expenseOnly = snapshot.filter == ViewFilter.EXPENSES,
                incomeOnly = snapshot.filter == ViewFilter.INCOMES,
                categoryIds = snapshot.selectedCategoryIds.toList(),
            ).onSuccess { items ->
                _state.value = _state.value.copy(isLoading = false, items = items)
            }.onFailure { throwable ->
                _state.value = _state.value.copy(isLoading = false, error = throwable.message)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().onSuccess { categories ->
                _state.value = _state.value.copy(
                    categories = categories.filter { it.name != "UNCLASSIFIED" },
                )
            }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deletePaymentItem(id)
                .onSuccess { refresh() }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(error = throwable.message)
                }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    repository: FinanceRepository,
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenCategoryTypes: () -> Unit,
    onOpenStatistics: () -> Unit,
) {
    val viewModel: SummaryViewModel =
        viewModel(factory = simpleFactory { SummaryViewModel(repository) })
    val state by viewModel.state.collectAsState()
    var itemPendingDelete by remember { mutableStateOf<PaymentItem?>(null) }

    // Reload when returning from add/edit screens
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FinanceBook", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = onOpenStatistics) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Statistics")
                    }
                    IconButton(onClick = onOpenCategories) {
                        Icon(Icons.Default.Category, contentDescription = "Categories")
                    }
                    IconButton(onClick = onOpenCategoryTypes) {
                        Icon(Icons.Default.Label, contentDescription = "Category types")
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPayment,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add payment")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── View filter chips ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ViewFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label) },
                    )
                }
            }

            // ── Category filter chips ───────────────────────────────
            if (state.filter != ViewFilter.FEES && state.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = category.id in state.selectedCategoryIds,
                            onClick = { viewModel.toggleCategory(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                    if (state.selectedCategoryIds.isNotEmpty()) {
                        AssistChip(
                            onClick = { viewModel.clearCategoryFilter() },
                            label = { Text("Reset") },
                        )
                    }
                }
            }

            when {
                state.isLoading && state.items.isEmpty() -> LoadingBox()
                state.error != null -> ErrorText(state.error ?: "")
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.visibleItems, key = { it.id }) { item ->
                            PaymentCard(
                                item = item,
                                feeMode = state.filter == ViewFilter.FEES,
                                onClick = { onEditPayment(item.id) },
                                onDelete = { itemPendingDelete = item },
                            )
                        }
                        item {
                            SumCard(total = state.total, feeMode = state.filter == ViewFilter.FEES)
                        }
                    }
                }
            }
        }
    }

    // ── Delete confirmation ─────────────────────────────────────────
    itemPendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            title = { Text("Delete payment?") },
            text = {
                Text(
                    "This removes \"${item.description ?: formatAmount(item.amount)}\" " +
                        "and refunds any transaction fee.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(item.id)
                    itemPendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

private val ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", Locale.ENGLISH)

private fun formatDate(raw: String): String = runCatching {
    LocalDateTime.parse(raw.substringBefore('+').substringBefore('Z'), ISO_FORMAT)
        .format(DISPLAY_FORMAT)
}.getOrDefault(raw)

@Composable
private fun PaymentCard(
    item: PaymentItem,
    feeMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDate(item.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.periodic && !feeMode) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Periodic payment",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!feeMode) {
                    if (!item.description.isNullOrBlank()) {
                        Text(
                            item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                    item.recipient?.let { recipient ->
                        Text(
                            (if (item.isExpense) "To: " else "From: ") + recipient.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item.standardCategory?.let { category ->
                        Text(
                            category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (feeMode) {
                    AmountText(item.transactionFee ?: 0.0, forceNegativeColor = true)
                } else {
                    AmountText(item.amount)
                    item.transactionFee?.let { fee ->
                        Text(
                            "fee ${formatAmount(fee)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!feeMode) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete payment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SumCard(total: Double, feeMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color444, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "SUM",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        AmountText(total, forceNegativeColor = feeMode)
    }
}

private val Color444 = androidx.compose.ui.graphics.Color(0xFF444444)
