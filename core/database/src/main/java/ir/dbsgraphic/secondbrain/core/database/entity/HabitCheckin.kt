package ir.dbsgraphic.secondbrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * One day's completion of a habit. A habit itself is just an Item (type=habit);
 * its check-ins are stored here — real rows, not a JSON blob — so streaks and
 * history are plain queries (§4, §5). [dayStart] is the local start-of-day millis.
 */
@Entity(
    tableName = "habit_checkins",
    primaryKeys = ["habitId", "dayStart"],
    indices = [Index("habitId")],
)
@Serializable
data class HabitCheckin(
    val habitId: String,
    val dayStart: Long,
    val doneAt: Long,
)
