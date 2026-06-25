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

    /**
     * Capture something shared from another app — text, or an image/file blob
     * (capturedVia = share). Still lands formless in the Inbox (§3).
     */
    suspend fun captureShared(
        content: String,
        blobRef: String? = null,
        contentType: String = "text",
    ): String

    /**
     * Capture a media blob recorded/taken in-app — voice or photo. Lands
     * formless in the Inbox; [capturedVia] records the surface (voice|photo).
     */
    suspend fun captureBlob(
        blobRef: String,
        contentType: String,
        capturedVia: String,
        content: String = "",
    ): String

    /** Replace an item's text — used by AI enrichment (transcription, OCR). */
    suspend fun updateContent(id: String, content: String)

    // ── Trash: nothing is ever lost (Constitution §13) ──────────────────────

    /** Items currently in the Trash. */
    fun observeTrash(): Flow<List<Item>>

    /** Soft-delete: move to Trash (recoverable). */
    suspend fun trash(id: String)

    /** Restore from Trash to its prior place (project → triaged, else inbox). */
    suspend fun restore(id: String)

    /** Permanently delete one item (and its blob). */
    suspend fun deleteForever(id: String)

    /** Permanently empty the Trash (and blobs). */
    suspend fun emptyTrash()

    fun observeById(id: String): Flow<Item?>

    /** The whole life in reverse-chronological order — the Timeline (§19). */
    fun observeTimeline(): Flow<List<Item>>

    /** Instant ranked full-text search with Persian normalization (§6). */
    fun search(rawQuery: String): Flow<List<Item>>

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
