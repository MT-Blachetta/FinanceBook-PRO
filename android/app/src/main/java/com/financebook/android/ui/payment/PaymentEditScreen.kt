package com.financebook.android.ui.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.financebook.android.data.model.*
import com.financebook.android.data.repository.FinanceRepository
import com.financebook.android.ui.components.LoadingBox
import com.financebook.android.ui.login.simpleFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Add / edit payment form, mirroring the web PaymentItemForm:
 * amount + income/expense toggle, date, description, periodic flag,
 * recipient picker (with inline creation) and one category per type.
 */
data class PaymentEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    // form fields
    val amountText: String = "",
    val isExpense: Boolean = true,
    val date: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0),
    val description: String = "",
    val periodic: Boolean = false,
    val recipientId: Long? = null,
    val newRecipientName: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    // reference data
    val recipients: List<Recipient> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryTypes: List<CategoryType> = emptyList(),
) {
    val amountValue: BigDecimal?
        get() = amountText.replace(',', '.').toBigDecimalOrNull()?.abs()

    val canSave: Boolean
        get() = !isSaving && amountValue != null && amountValue!!.signum() > 0
}

private fun String.toBigDecimalOrNull(): BigDecimal? =
    runCatching { BigDecimal(this) }.getOrNull()

class PaymentEditViewModel(
    private val repository: FinanceRepository,
    private val itemId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentEditUiState())
    val state: StateFlow<PaymentEditUiState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val recipients = repository.getRecipients().getOrDefault(emptyList())
            val categories = repository.getCategories().getOrDefault(emptyList())
            val types = repository.getCategoryTypes().getOrDefault(emptyList())

            var next = _state.value.copy(
                recipients = recipients,
                categories = categories,
                categoryTypes = types,
                isLoading = itemId != null,
            )
            _state.value = next

            if (itemId != null) {
                repository.getPaymentItem(itemId)
                    .onSuccess { item ->
                        next = next.copy(
                            isLoading = false,
                            amountText = String.format(java.util.Locale.ROOT, "%.2f", abs(item.amount)),
                            isExpense = item.amount < 0,
                            date = runCatching {
                                LocalDateTime.parse(
                                    item.date.substringBefore('+').substringBefore('Z'),
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                                )
                            }.getOrDefault(LocalDateTime.now()),
                            description = item.description ?: "",
                            periodic = item.periodic,
                            recipientId = item.recipientId,
                            selectedCategoryIds = item.categories.map { it.id }.toSet(),
                        )
                        _state.value = next
                    }
                    .onFailure { throwable ->
                        _state.value = next.copy(isLoading = false, error = throwable.message)
                    }
            }
        }
    }

    fun update(transform: (PaymentEditUiState) -> PaymentEditUiState) {
        _state.value = transform(_state.value)
    }

    /** Select a category; only one per category type is allowed (server rule). */
    fun toggleCategory(category: Category) {
        val current = _state.value
        val selected = current.selectedCategoryIds.toMutableSet()
        if (category.id in selected) {
            selected.remove(category.id)
        } else {
            // Drop a previously selected category of the same type
            current.categories
                .filter { it.typeId == category.typeId && it.id in selected }
                .forEach { selected.remove(it.id) }
            selected.add(category.id)
        }
        _state.value = current.copy(selectedCategoryIds = selected)
    }

    fun createRecipient() {
        val name = _state.value.newRecipientName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            repository.createRecipient(RecipientCreate(name = name))
                .onSuccess { recipient ->
                    _state.value = _state.value.copy(
                        recipients = _state.value.recipients + recipient,
                        recipientId = recipient.id,
                        newRecipientName = "",
                    )
                }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(error = throwable.message)
                }
        }
    }

    fun save(onDone: () -> Unit) {
        val current = _state.value
        val amount = current.amountValue ?: return
        val signedAmount = if (current.isExpense) amount.negate() else amount
        val dateString = current.date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        _state.value = current.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val result = if (itemId == null) {
                repository.createPaymentItem(
                    PaymentItemCreate(
                        amount = signedAmount.toPlainString(),
                        date = dateString,
                        periodic = current.periodic,
                        description = current.description.ifBlank { null },
                        recipientId = current.recipientId,
                        categoryIds = current.selectedCategoryIds.toList().ifEmpty { null },
                    ),
                ).map { }
            } else {
                repository.updatePaymentItem(
                    itemId,
                    PaymentItemUpdate(
                        amount = signedAmount.toPlainString(),
                        date = dateString,
                        periodic = current.periodic,
                        description = current.description,
                        recipientId = current.recipientId,
                        categoryIds = current.selectedCategoryIds.toList(),
                    ),
                ).map { }
            }

            result
                .onSuccess {
                    _state.value = _state.value.copy(isSaving = false)
                    onDone()
                }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(isSaving = false, error = throwable.message)
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEditScreen(
    repository: FinanceRepository,
    itemId: Long?,
    onDone: () -> Unit,
) {
    val viewModel: PaymentEditViewModel = viewModel(
        key = "payment-$itemId",
        factory = simpleFactory { PaymentEditViewModel(repository, itemId) },
    )
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "Add Payment" else "Edit Payment") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Amount + direction ──────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = { value -> viewModel.update { it.copy(amountText = value) } },
                    label = { Text("Amount (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    isError = state.amountText.isNotEmpty() && state.amountValue == null,
                )
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = state.isExpense,
                        onClick = { viewModel.update { it.copy(isExpense = true) } },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.error,
                            activeContentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text("Expense") }
                    SegmentedButton(
                        selected = !state.isExpense,
                        onClick = { viewModel.update { it.copy(isExpense = false) } },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) { Text("Income") }
                }
            }

            // ── Date ────────────────────────────────────────────────
            OutlinedTextField(
                value = state.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Change") }
                },
            )

            // ── Description ─────────────────────────────────────────
            OutlinedTextField(
                value = state.description,
                onValueChange = { value -> viewModel.update { it.copy(description = value) } },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Periodic ────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.periodic,
                    onCheckedChange = { value -> viewModel.update { it.copy(periodic = value) } },
                )
                Text("Periodic (recurring) payment")
            }

            // ── Recipient ───────────────────────────────────────────
            Text("Recipient", style = MaterialTheme.typography.titleSmall)
            RecipientPicker(state = state, viewModel = viewModel)

            // ── Categories, one per type ────────────────────────────
            if (state.categoryTypes.isNotEmpty()) {
                Text("Categories (one per type)", style = MaterialTheme.typography.titleSmall)
                state.categoryTypes.forEach { type ->
                    val categoriesOfType = state.categories.filter { it.typeId == type.id }
                    if (categoriesOfType.isNotEmpty()) {
                        Text(
                            type.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRowChips(
                            categories = categoriesOfType,
                            selectedIds = state.selectedCategoryIds,
                            onToggle = viewModel::toggleCategory,
                        )
                    }
                }
            }

            if (state.error != null) {
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (itemId == null) "Create Payment" else "Save Changes")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.toLocalDate()
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        viewModel.update {
                            it.copy(date = LocalDateTime.of(picked, it.date.toLocalTime()))
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipientPicker(state: PaymentEditUiState, viewModel: PaymentEditViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = state.recipients.firstOrNull { it.id == state.recipientId }?.name
        ?: "No recipient"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select recipient") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("No recipient") },
                onClick = {
                    viewModel.update { it.copy(recipientId = null) }
                    expanded = false
                },
            )
            state.recipients.forEach { recipient ->
                DropdownMenuItem(
                    text = { Text(recipient.name) },
                    onClick = {
                        viewModel.update { it.copy(recipientId = recipient.id) }
                        expanded = false
                    },
                )
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = state.newRecipientName,
            onValueChange = { value -> viewModel.update { it.copy(newRecipientName = value) } },
            label = { Text("New recipient name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { viewModel.createRecipient() },
            enabled = state.newRecipientName.isNotBlank(),
        ) { Text("Add") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(
    categories: List<Category>,
    selectedIds: Set<Long>,
    onToggle: (Category) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            FilterChip(
                selected = category.id in selectedIds,
                onClick = { onToggle(category) },
                label = { Text(category.name) },
            )
        }
    }
}
