package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.dao.HabitCheckinDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.entity.HabitCheckin
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Medicines as a vertical over the one pipeline (Phase 16). A medicine is an
 * Item (type=medicine): its dosage/schedule/stock live in [MedicineDetails], the
 * next dose reuses [Item.reminderAt] (notifies for free), and each dose taken is
 * recorded in the shared `habit_checkins` table — so adherence streaks are the
 * very same engine that powers Habits (§4: one atom, no duplication).
 */
interface MedicineRepository {

    /** Medicines, oldest first (stable list order). */
    fun observeMedicines(): Flow<List<Item>>

    /** Adherence check-ins across all medicines (and habits — same table). */
    fun observeCheckins(): Flow<List<HabitCheckin>>

    suspend fun createMedicine(
        name: String,
        dosage: String,
        dosesPerDay: Int,
        stock: Int,
        refillAt: Int,
    ): String

    /** Record a dose taken: mark today, decrement stock, roll the reminder on. */
    suspend fun logDose(id: String)

    /** Add [amount] doses to stock (a refill). */
    suspend fun refill(id: String, amount: Int)
}

class MedicineRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val habitCheckinDao: HabitCheckinDao,
    private val itemRepository: ItemRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : MedicineRepository {

    override fun observeMedicines(): Flow<List<Item>> = itemDao.observeByType("medicine")

    override fun observeCheckins(): Flow<List<HabitCheckin>> = habitCheckinDao.observeAll()

    override suspend fun createMedicine(
        name: String,
        dosage: String,
        dosesPerDay: Int,
        stock: Int,
        refillAt: Int,
    ): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Medicine name cannot be empty" }
        val now = clock.now()
        val times = MedicineSchedule.defaultTimes(dosesPerDay)
        val details = MedicineDetails(
            dosage = dosage.trim(),
            times = times,
            stock = stock.coerceAtLeast(0),
            refillAt = refillAt.coerceAtLeast(0),
        )
        val next = MedicineSchedule.nextDose(times, now)
        val item = Item(
            id = idGenerator.newId(),
            createdAt = now,
            updatedAt = now,
            content = trimmed,
            type = "medicine",
            status = "triaged",
            details = MedicineCodec.encode(details),
            capturedVia = "quickAdd",
            reminderAt = next,
        )
        itemDao.upsert(item)
        if (next != null) itemRepository.setReminder(item.id, next)
        return item.id
    }

    override suspend fun logDose(id: String) {
        val item = itemDao.getById(id) ?: return
        if (item.type != "medicine") return
        val details = MedicineCodec.decode(item.details)
        val now = clock.now()
        // Mark today's adherence (idempotent — one row per day, reusing the
        // habit cadence table) and spend one dose from stock.
        val dayStart = DayUtil.startOfDay(now)
        if (habitCheckinDao.get(id, dayStart) == null) {
            habitCheckinDao.upsert(HabitCheckin(habitId = id, dayStart = dayStart, doneAt = now))
        }
        val updated = details.copy(stock = (details.stock - 1).coerceAtLeast(0))
        itemDao.update(item.copy(details = MedicineCodec.encode(updated), updatedAt = now))
        // Roll the reminder forward to the next scheduled dose.
        val next = MedicineSchedule.nextDose(updated.times, now)
        itemRepository.setReminder(id, next)
    }

    override suspend fun refill(id: String, amount: Int) {
        if (amount <= 0) return
        val item = itemDao.getById(id) ?: return
        if (item.type != "medicine") return
        val details = MedicineCodec.decode(item.details)
        val updated = details.copy(stock = details.stock + amount)
        itemDao.update(item.copy(details = MedicineCodec.encode(updated), updatedAt = clock.now()))
    }
}
