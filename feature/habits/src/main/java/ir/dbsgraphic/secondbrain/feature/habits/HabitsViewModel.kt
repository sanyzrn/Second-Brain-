package ir.dbsgraphic.secondbrain.feature.habits

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.dbsgraphic.secondbrain.core.data.DayUtil
import ir.dbsgraphic.secondbrain.core.data.HabitRepository
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HabitUi(val item: Item, val doneToday: Boolean, val streak: Int)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
) : ViewModel() {

    val habits: StateFlow<List<HabitUi>> = combine(
        habitRepository.observeHabits(),
        habitRepository.observeCheckins(),
    ) { habits, checkins ->
        val daysByHabit = checkins.groupBy { it.habitId }
            .mapValues { entry -> entry.value.map { it.dayStart }.toSet() }
        habits.map { habit ->
            val days = daysByHabit[habit.id] ?: emptySet()
            HabitUi(item = habit, doneToday = DayUtil.startOfToday() in days, streak = streakOf(days))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { runCatching { habitRepository.createHabit(name) } }
    }

    fun toggle(habitId: String) {
        viewModelScope.launch { habitRepository.toggleToday(habitId) }
    }

    /** Consecutive completed days ending today (or yesterday, as a one-day grace). */
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
