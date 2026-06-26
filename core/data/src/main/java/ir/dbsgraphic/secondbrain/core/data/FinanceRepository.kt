package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Finance (expenses & installments) as a vertical over the one pipeline. A
 * finance entry is an Item (type=expense | installment) whose money lives in
 * [FinanceDetails] and whose due date reuses [Item.reminderAt] — so it is
 * searchable, openable in detail, trashable, backed up, and reminded for free
 * (Constitution §4: one atom, no duplication).
 */
interface FinanceRepository {

    /** Expenses and installment plans, newest first. */
    fun observeFinance(): Flow<List<Item>>

    /** A one-off expense. [dueAt] optionally schedules a reminder. */
    suspend fun createExpense(title: String, amount: Long, dueAt: Long? = null): String

    /**
     * An installment plan: [count] payments of [perAmount] Tomans, every
     * [periodDays] days. The first due date (if any) schedules a reminder, and
     * each payment advances it (see [pay]).
     */
    suspend fun createInstallment(
        title: String,
        perAmount: Long,
        count: Int,
        periodDays: Int,
        firstDueAt: Long? = null,
    ): String

    /**
     * Record one payment: increments paid, and rolls the due date forward by the
     * plan's period (or clears it once finished, which cancels the reminder).
     */
    suspend fun pay(id: String)
}

class FinanceRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val itemRepository: ItemRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : FinanceRepository {

    override fun observeFinance(): Flow<List<Item>> = itemDao.observeFinance()

    override suspend fun createExpense(title: String, amount: Long, dueAt: Long?): String {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "Expense title cannot be empty" }
        require(amount > 0) { "Amount must be positive" }
        val now = clock.now()
        val details = FinanceDetails(amount = amount, installmentTotal = 1, installmentsPaid = 0)
        val item = Item(
            id = idGenerator.newId(),
            createdAt = now,
            updatedAt = now,
            content = trimmed,
            type = "expense",
            status = "triaged",
            details = FinanceCodec.encode(details),
            capturedVia = "quickAdd",
            reminderAt = dueAt,
        )
        itemDao.upsert(item)
        // Route the due date through the repository so the notification is scheduled.
        if (dueAt != null) itemRepository.setReminder(item.id, dueAt)
        return item.id
    }

    override suspend fun createInstallment(
        title: String,
        perAmount: Long,
        count: Int,
        periodDays: Int,
        firstDueAt: Long?,
    ): String {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "Installment title cannot be empty" }
        require(perAmount > 0) { "Amount must be positive" }
        require(count >= 1) { "Installment count must be at least 1" }
        val period = periodDays.coerceAtLeast(1)
        val now = clock.now()
        val details = FinanceDetails(
            amount = perAmount,
            installmentTotal = count,
            installmentsPaid = 0,
            periodDays = period,
        )
        val item = Item(
            id = idGenerator.newId(),
            createdAt = now,
            updatedAt = now,
            content = trimmed,
            type = "installment",
            status = "triaged",
            details = FinanceCodec.encode(details),
            capturedVia = "quickAdd",
            reminderAt = firstDueAt,
        )
        itemDao.upsert(item)
        if (firstDueAt != null) itemRepository.setReminder(item.id, firstDueAt)
        return item.id
    }

    override suspend fun pay(id: String) {
        val item = itemDao.getById(id) ?: return
        val details = FinanceCodec.decode(item.details)
        if (details.isDone) return
        val updated = details.copy(installmentsPaid = details.installmentsPaid + 1)
        val now = clock.now()
        itemDao.update(
            item.copy(
                details = FinanceCodec.encode(updated),
                updatedAt = now,
            ),
        )
        // Advance the due date to the next installment, or clear it when finished.
        val nextDue = if (updated.isDone) {
            null
        } else {
            item.reminderAt?.plus(updated.periodDays.toLong() * MILLIS_PER_DAY)
        }
        itemRepository.setReminder(id, nextDue)
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
