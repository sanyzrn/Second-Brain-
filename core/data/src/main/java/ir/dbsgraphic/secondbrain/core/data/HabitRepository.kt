package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.dao.HabitCheckinDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.entity.HabitCheckin
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Habits are Items (type=habit); their daily completions live in
 * [HabitCheckin]. The vertical reuses the whole pipeline — a habit is openable
 * in detail, searchable, trashable and backed up like any item.
 */
interface HabitRepository {
    fun observeHabits(): Flow<List<Item>>
    fun observeCheckins(): Flow<List<HabitCheckin>>
    suspend fun createHabit(name: String): String
    /** Toggle today's completion for a habit. */
    suspend fun toggleToday(habitId: String)
}

class HabitRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val habitCheckinDao: HabitCheckinDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : HabitRepository {

    override fun observeHabits(): Flow<List<Item>> = itemDao.observeByType("habit")

    override fun observeCheckins(): Flow<List<HabitCheckin>> = habitCheckinDao.observeAll()

    override suspend fun createHabit(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Habit name cannot be empty" }
        val now = clock.now()
        val item = Item(
            id = idGenerator.newId(),
            createdAt = now,
            updatedAt = now,
            content = trimmed,
            type = "habit",
            status = "triaged",
            details = """{"cadence":"daily"}""",
            capturedVia = "quickAdd",
        )
        itemDao.upsert(item)
        return item.id
    }

    override suspend fun toggleToday(habitId: String) {
        val dayStart = DayUtil.startOfDay(clock.now())
        if (habitCheckinDao.get(habitId, dayStart) != null) {
            habitCheckinDao.delete(habitId, dayStart)
        } else {
            habitCheckinDao.upsert(HabitCheckin(habitId = habitId, dayStart = dayStart, doneAt = clock.now()))
        }
    }
}
