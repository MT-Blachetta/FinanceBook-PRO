package com.financebook.android.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.financebook.android.data.model.Statistics
import com.financebook.android.data.repository.FinanceRepository
import com.financebook.android.ui.components.ErrorText
import com.financebook.android.ui.components.LoadingBox
import com.financebook.android.ui.components.formatAmount
import com.financebook.android.ui.login.simpleFactory
import com.financebook.android.ui.theme.CardBackground
import com.financebook.android.ui.theme.Negative
import com.financebook.android.ui.theme.Positive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Statistics view backed by the server-side GET /statistics endpoint:
 * totals, a monthly income/expense bar chart, and a category breakdown.
 */
sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState
    data class Error(val message: String) : StatisticsUiState
    data class Ready(val statistics: Statistics) : StatisticsUiState
}

class StatisticsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val state: StateFlow<StatisticsUiState> = _state

    init {
        load()
    }

    fun load() {
        _state.value = StatisticsUiState.Loading
        viewModelScope.launch {
            repository.getStatistics()
                .onSuccess { _state.value = StatisticsUiState.Ready(it) }
                .onFailure {
                    _state.value = StatisticsUiState.Error(it.message ?: "Failed to load")
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    repository: FinanceRepository,
    onBack: () -> Unit,
) {
    val viewModel: StatisticsViewModel =
        viewModel(factory = simpleFactory { StatisticsViewModel(repository) })
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
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
        when (val current = state) {
            is StatisticsUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is StatisticsUiState.Error -> Column(Modifier.padding(padding)) {
                ErrorText(current.message)
                TextButton(
                    onClick = { viewModel.load() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Retry") }
            }
            is StatisticsUiState.Ready -> StatisticsContent(
                statistics = current.statistics,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun StatisticsContent(statistics: Statistics, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Totals ──────────────────────────────────────────────────
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TotalCard("Income", statistics.totalIncome, Positive, Modifier.weight(1f))
                TotalCard("Expenses", -statistics.totalExpenses, Negative, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TotalCard(
                    "Balance",
                    statistics.netBalance,
                    if (statistics.netBalance >= 0) Positive else Negative,
                    Modifier.weight(1f),
                )
                TotalCard("Fees", -statistics.totalFees, Negative, Modifier.weight(1f))
            }
        }

        // ── Monthly chart ───────────────────────────────────────────
        if (statistics.monthly.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "MONTHLY INCOME / EXPENSES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        MonthlyBarChart(statistics)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                statistics.monthly.first().month,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                statistics.monthly.last().month,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // ── Category breakdown ──────────────────────────────────────
        if (statistics.byCategory.isNotEmpty()) {
            item {
                Text(
                    "BY CATEGORY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(statistics.byCategory, key = { it.categoryId ?: -1L }) { bucket ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(bucket.categoryName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${bucket.count} payments",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (bucket.income > 0) {
                                Text(
                                    "+${formatAmount(bucket.income)}",
                                    color = Positive,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (bucket.expenses > 0) {
                                Text(
                                    "-${formatAmount(bucket.expenses)}",
                                    color = Negative,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "${statistics.itemCount} payments in total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TotalCard(label: String, value: Double, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = CardBackground)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatAmount(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

/** Paired income/expense bars per month, drawn with plain Compose Canvas. */
@Composable
private fun MonthlyBarChart(statistics: Statistics) {
    val months = statistics.monthly
    val maxValue = months.maxOf { max(it.income, it.expenses) }.takeIf { it > 0 } ?: 1.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        val groupWidth = size.width / months.size
        val barWidth = (groupWidth * 0.35f).coerceAtMost(48f)
        val gap = groupWidth * 0.08f

        months.forEachIndexed { index, bucket ->
            val groupStart = index * groupWidth + groupWidth / 2f

            val incomeHeight = (bucket.income / maxValue * size.height).toFloat()
            drawRect(
                color = Positive,
                topLeft = Offset(groupStart - barWidth - gap / 2f, size.height - incomeHeight),
                size = Size(barWidth, incomeHeight),
            )

            val expenseHeight = (bucket.expenses / maxValue * size.height).toFloat()
            drawRect(
                color = Negative,
                topLeft = Offset(groupStart + gap / 2f, size.height - expenseHeight),
                size = Size(barWidth, expenseHeight),
            )
        }
    }
}
