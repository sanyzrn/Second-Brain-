package ir.dbsgraphic.secondbrain.feature.medicine

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dbsgraphic.secondbrain.core.data.DayUtil
import ir.dbsgraphic.secondbrain.core.data.MedicineCodec
import ir.dbsgraphic.secondbrain.core.data.MedicineDetails
import ir.dbsgraphic.secondbrain.core.data.MedicineRepository
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class MedicineUi(
    val item: Item,
    val details: MedicineDetails,
    val nextDose: Long?,
    val takenToday: Boolean,
    val streak: Int,
)

@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
) : ViewModel() {

    val medicines: StateFlow<List<MedicineUi>> = combine(
        medicineRepository.observeMedicines(),
        medicineRepository.observeCheckins(),
    ) { medicines, checkins ->
        val daysById = checkins.groupBy { it.habitId }
            .mapValues { entry -> entry.value.map { it.dayStart }.toSet() }
        medicines.map { med ->
            val days = daysById[med.id] ?: emptySet()
            MedicineUi(
                item = med,
                details = MedicineCodec.decode(med.details),
                nextDose = med.reminderAt,
                takenToday = DayUtil.startOfToday() in days,
                streak = streakOf(days),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, dosage: String, dosesPerDay: Int, stock: Int, refillAt: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { medicineRepository.createMedicine(name, dosage, dosesPerDay, stock, refillAt) }
        }
    }

    fun logDose(id: String) {
        viewModelScope.launch { medicineRepository.logDose(id) }
    }

    fun refill(id: String, amount: Int) {
        viewModelScope.launch { medicineRepository.refill(id, amount) }
    }

    /** Consecutive adherence days ending today (or yesterday, one-day grace). */
    private fun streakOf(days: Set<Long>): Int {
        if (days.isEmpty()) return 0
        var day = DayUtil.startOfToday()
        if (day !in days) day = DayUtil.previousDay(day)
        var streak = 0
        while (day in days) {
            streak++
            day = DayUtil.previousDay(day)
        }
        return streak
    }
}
