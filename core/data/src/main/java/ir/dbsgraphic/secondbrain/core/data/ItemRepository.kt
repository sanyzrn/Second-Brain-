package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.flow.Flow

/**
 * The single doorway to Items for the whole app (Constitution §4: stored once,
 * all modules read one source). Feature modules never touch Room directly.
 */
interface ItemRepository {

    /** The Inbox stream — formless items, newest first. */
    fun observeInbox(): Flow<List<Item>>

    /** Live count of inbox items, for badges/headers. */
    fun observeInboxCount(): Flow<Int>

    /**
     * Capture a raw thought. No decisions are made here — the item lands in the
     * Inbox formless (`status = inbox`, `type = null`) so capture stays faster
     * than forgetting (Constitution §2, §3). Returns the new item's id.
     */
    suspend fun capture(content: String): String

    fun observeById(id: String): Flow<Item?>

    /** A project's contents, derived by filtering Items (§4). */
    fun observeByProject(projectId: String): Flow<List<Item>>

    /**
     * Triage a formless inbox item: give it a type (and optionally a project and
     * tags), and move it out of the Inbox. Organizing happens here, never at
     * capture time (§3, §20).
     */
    suspend fun triage(
        itemId: String,
        type: String,
        projectId: String? = null,
        tags: List<String> = emptyList(),
    )

    // ── Connections (§5) ────────────────────────────────────────────────────

    /** Items that point at [itemId] — backlinks. */
    fun observeBacklinks(itemId: String): Flow<List<Item>>

    /** Items that [itemId] points at. */
    fun observeOutgoing(itemId: String): Flow<List<Item>>

    suspend fun link(fromId: String, toId: String, kind: String = "related")

    suspend fun unlink(fromId: String, toId: String, kind: String = "related")
}
