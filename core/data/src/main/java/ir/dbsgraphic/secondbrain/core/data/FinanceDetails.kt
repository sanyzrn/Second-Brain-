package ir.dbsgraphic.secondbrain.core.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The finance payload carried in an Item's [Item.details] JSON. A finance item is
 * an ordinary Item (type=expense for a one-off, type=installment for a plan); its
 * money lives here while its due date reuses [Item.reminderAt] — so a finance
 * item gets reminders, search, trash and backup for free.
 *
 * [amount] is the per-payment amount in Tomans (the whole, indivisible unit in
 * Iran day-to-day). For a one-off expense [installmentTotal] is 1.
 */
@Serializable
data class FinanceDetails(
    val amount: Long = 0,
    val installmentTotal: Int = 1,
    val installmentsPaid: Int = 0,
    val periodDays: Int = 30,
) {
    /** Installments still owed. */
    val remainingCount: Int get() = (installmentTotal - installmentsPaid).coerceAtLeast(0)

    /** Money still owed, in Tomans. */
    val remainingAmount: Long get() = remainingCount.toLong() * amount

    /** The full plan amount, in Tomans. */
    val totalAmount: Long get() = installmentTotal.toLong() * amount

    val isDone: Boolean get() = installmentsPaid >= installmentTotal
}

/** Reads/writes [FinanceDetails] tolerantly so older items never break. */
object FinanceCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun decode(details: String?): FinanceDetails =
        if (details.isNullOrBlank()) FinanceDetails()
        else runCatching { json.decodeFromString(FinanceDetails.serializer(), details) }
            .getOrDefault(FinanceDetails())

    fun encode(details: FinanceDetails): String =
        json.encodeToString(FinanceDetails.serializer(), details)
}
