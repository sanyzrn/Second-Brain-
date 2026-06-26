package ir.dbsgraphic.secondbrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.dbsgraphic.secondbrain.core.database.entity.HabitCheckin
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCheckinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkin: HabitCheckin)

    @Query("SELECT * FROM habit_checkins WHERE habitId = :habitId AND dayStart = :dayStart")
    suspend fun get(habitId: String, dayStart: Long): HabitCheckin?

    @Query("DELETE FROM habit_checkins WHERE habitId = :habitId AND dayStart = :dayStart")
    suspend fun delete(habitId: String, dayStart: Long)

    @Query("DELETE FROM habit_checkins WHERE habitId = :habitId")
    suspend fun deleteForHabit(habitId: String)

    @Query("SELECT * FROM habit_checkins")
    fun observeAll(): Flow<List<HabitCheckin>>

    @Query("SELECT * FROM habit_checkins")
    suspend fun getAll(): List<HabitCheckin>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(checkins: List<HabitCheckin>)
}
