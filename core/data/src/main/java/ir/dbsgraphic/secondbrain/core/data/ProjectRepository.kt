package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.dao.ProjectDao
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.Project
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Doorway to Projects (hubs). Item contents are read via [ItemRepository]. */
interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    fun observeProject(id: String): Flow<Project?>
    fun observeItemCount(id: String): Flow<Int>
    fun observeProjectItems(id: String): Flow<List<Item>>
    suspend fun createProject(name: String): String
}

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val itemDao: ItemDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : ProjectRepository {

    override fun observeProjects(): Flow<List<Project>> = projectDao.observeActive()

    override fun observeProject(id: String): Flow<Project?> = projectDao.observeById(id)

    override fun observeItemCount(id: String): Flow<Int> = projectDao.observeItemCount(id)

    override fun observeProjectItems(id: String): Flow<List<Item>> =
        itemDao.observeByProject(id)

    override suspend fun createProject(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Project name cannot be empty" }
        val now = clock.now()
        val project = Project(
            id = idGenerator.newId(),
            name = trimmed,
            createdAt = now,
            updatedAt = now,
        )
        projectDao.upsert(project)
        return project.id
    }
}
