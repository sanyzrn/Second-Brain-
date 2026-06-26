package ir.dbsgraphic.secondbrain.feature.finance

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dbsgraphic.secondbrain.core.data.FinanceCodec
import ir.dbsgraphic.secondbrain.core.data.FinanceDetails
import ir.dbsgraphic.secondbrain.core.data.FinanceRepository
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One finance row, decoded and ready to render. */
@Immutable
data class FinanceUi(
    val item: Item,
    val finance: FinanceDetails,
    val dueAt: Long?,
) {
    val isInstallment: Boolean get() = item.type == "installment"
}

/** A finance screen's whole state: rows + the live total still owed. */
@Immutable
data class FinanceState(
    val rows: List<FinanceUi> = emptyList(),
    val totalRemaining: Long = 0,
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    val state: StateFlow<FinanceState> = financeRepository.observeFinance()
        .map { items ->
            val rows = items.map { item ->
                FinanceUi(
                    item = item,
                    finance = FinanceCodec.decode(item.details),
                    dueAt = item.reminderAt,
                )
            }
            // Outstanding entries first (by due date), then the settled ones.
            val sorted = rows.sortedWith(
                compareBy({ it.finance.isDone }, { it.dueAt ?: Long.MAX_VALUE }),
            )
            FinanceState(
                rows = sorted,
                totalRemaining = rows.sumOf { it.finance.remainingAmount },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceState())

    fun addExpense(title: String, amount: Long, dueAt: Long?) {
        if (title.isBlank() || amount <= 0) return
        viewModelScope.launch { runCatching { financeRepository.createExpense(title, amount, dueAt) } }
    }

    fun addInstallment(title: String, perAmount: Long, count: Int, periodDays: Int, firstDueAt: Long?) {
        if (title.isBlank() || perAmount <= 0 || count < 1) return
        viewModelScope.launch {
            runCatching { financeRepository.createInstallment(title, perAmount, count, periodDays, firstDueAt) }
        }
    }

    fun pay(id: String) {
        viewModelScope.launch { financeRepository.pay(id) }
    }
}
