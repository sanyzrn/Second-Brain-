package ir.dbsgraphic.secondbrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.dbsgraphic.secondbrain.core.database.entity.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: Project)

    @Query("SELECT * FROM projects WHERE status = 'active' ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: String): Flow<Project?>

    /** Live item count for a project — derived, never stored on the project. */
    @Query("SELECT COUNT(*) FROM items WHERE projectId = :id AND status != 'trashed'")
    fun observeItemCount(id: String): Flow<Int>

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<Project>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(projects: List<Project>)
}
