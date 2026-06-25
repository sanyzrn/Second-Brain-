package ir.dbsgraphic.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A Project is a **hub, not a silo** (Constitution §4). It stores only its own
 * metadata — name, status, an optional color. Its contents are never duplicated
 * here; the project's tasks/notes/docs are derived by filtering Items on
 * `projectId`.
 */
@Entity(
    tableName = "projects",
    indices = [Index("name")],
)
@Serializable
data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val color: String? = null,
    val status: String = "active", // active|archived|trashed
)
