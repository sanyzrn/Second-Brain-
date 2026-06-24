package ir.dbsgraphic.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * A real, queryable connection between two Items (Constitution §5: everything
 * connects, and the connections are the real value). Modeled as a join table —
 * not a JSON blob — so backlinks ("what points at this?") are answerable with a
 * plain SQL query.
 *
 * `kind` lets connections carry meaning later (related|reference|parent|…).
 */
@Entity(
    tableName = "item_links",
    primaryKeys = ["fromId", "toId", "kind"],
    indices = [Index("toId"), Index("fromId")],
)
data class ItemLink(
    val fromId: String,
    val toId: String,
    val kind: String = "related",
    val createdAt: Long,
)
