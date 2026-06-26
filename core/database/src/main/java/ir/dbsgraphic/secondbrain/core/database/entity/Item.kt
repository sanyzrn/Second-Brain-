package ir.dbsgraphic.secondbrain.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * The one atom of the system. An Item is born formless in the Inbox
 * (`status = inbox`, `type = null`) and is typed later, during triage
 * (Constitution §3: everything starts in the Inbox; §4: stored once).
 *
 * Every module reads this single source — notes, tasks, docs, project views
 * are all just filtered Items. Connections between Items live in a separate
 * queryable join table (added in Phase 2), not as a JSON blob (§5).
 */
@Entity(
    tableName = "items",
    indices = [
        Index("status"),
        Index("projectId"),
        Index("createdAt"),
    ],
)
@Serializable
data class Item(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val content: String,
    val blobRef: String? = null,              // voice / image / file / pdf
    val contentType: String = "text",         // text|voice|image|file|link|pdf|location
    val capturedVia: String = "quickAdd",     // quickAdd|share|voice|photo|widget
    val status: String = "inbox",             // inbox|triaged|archived|trashed
    val type: String? = null,                 // null until triaged → note|task|idea|doc|...
    val details: String? = null,              // JSON for type-specific fields
    @ColumnInfo(name = "projectId") val projectId: String? = null,
    val tags: String = "[]",
    val reminderAt: Long? = null,            // epoch millis of a scheduled reminder
)
