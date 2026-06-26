package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemLinkDao
import ir.dbsgraphic.secondbrain.core.database.dao.ProjectDao
import ir.dbsgraphic.secondbrain.core.database.dao.SearchDao
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.ItemLink
import ir.dbsgraphic.secondbrain.core.database.entity.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal class FakeItemDao : ItemDao {
    val items = MutableStateFlow<List<Item>>(emptyList())

    override suspend fun upsert(item: Item) {
        items.value = items.value.filterNot { it.id == item.id } + item
    }

    override suspend fun update(item: Item) = upsert(item)

    override suspend fun getById(id: String): Item? = items.value.find { it.id == id }

    override fun observeInbox(): Flow<List<Item>> =
        items.map { list -> list.filter { it.status == "inbox" }.sortedByDescending { it.createdAt } }

    override fun observeTimeline(): Flow<List<Item>> =
        items.map { list -> list.filter { it.status != "trashed" }.sortedByDescending { it.createdAt } }

    override fun observeById(id: String): Flow<Item?> =
        items.map { list -> list.find { it.id == id } }

    override fun observeByProject(projectId: String): Flow<List<Item>> =
        items.map { list ->
            list.filter { it.projectId == projectId && it.status != "trashed" }
                .sortedByDescending { it.createdAt }
        }

    override fun observeByType(type: String): Flow<List<Item>> =
        items.map { list ->
            list.filter { it.type == type && it.status != "trashed" }.sortedBy { it.createdAt }
        }

    override fun observeFinance(): Flow<List<Item>> =
        items.map { list ->
            list.filter { it.type in setOf("expense", "installment") && it.status != "trashed" }
                .sortedByDescending { it.createdAt }
        }

    override fun observeInboxCount(): Flow<Int> =
        items.map { list -> list.count { it.status == "inbox" } }

    override fun observeReminders(): Flow<List<Item>> =
        items.map { list ->
            list.filter { it.reminderAt != null && it.status != "trashed" }.sortedBy { it.reminderAt }
        }

    override fun observeTrashed(): Flow<List<Item>> =
        items.map { list -> list.filter { it.status == "trashed" }.sortedByDescending { it.updatedAt } }

    override suspend fun deleteById(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun deleteAllTrashed() {
        items.value = items.value.filterNot { it.status == "trashed" }
    }

    override suspend fun getAll(): List<Item> = items.value

    override suspend fun upsertAll(list: List<Item>) {
        list.forEach { upsert(it) }
    }
}

internal class FakeItemLinkDao(private val itemDao: FakeItemDao) : ItemLinkDao {
    val links = MutableStateFlow<List<ItemLink>>(emptyList())

    override suspend fun insert(link: ItemLink) {
        links.value = links.value.filterNot {
            it.fromId == link.fromId && it.toId == link.toId && it.kind == link.kind
        } + link
    }

    override suspend fun delete(fromId: String, toId: String, kind: String) {
        links.value = links.value.filterNot {
            it.fromId == fromId && it.toId == toId && it.kind == kind
        }
    }

    override fun observeBacklinks(id: String): Flow<List<Item>> =
        combine(links, itemDao.items) { ls, all ->
            ls.filter { it.toId == id }.mapNotNull { l -> all.find { it.id == l.fromId } }
        }

    override fun observeOutgoing(id: String): Flow<List<Item>> =
        combine(links, itemDao.items) { ls, all ->
            ls.filter { it.fromId == id }.mapNotNull { l -> all.find { it.id == l.toId } }
        }
}

internal class FakeSearchDao : SearchDao {
    override fun search(matchQuery: String, limit: Int): Flow<List<Item>> = flowOf(emptyList())
}

internal class FakeProjectDao : ProjectDao {
    val projects = MutableStateFlow<List<Project>>(emptyList())
    override suspend fun upsert(project: Project) {
        projects.value = projects.value.filterNot { it.id == project.id } + project
    }
    override fun observeActive(): Flow<List<Project>> =
        projects.map { list -> list.filter { it.status == "active" } }
    override fun observeById(id: String): Flow<Project?> =
        projects.map { list -> list.find { it.id == id } }
    override fun observeItemCount(id: String): Flow<Int> = flowOf(0)
    override suspend fun touch(id: String, now: Long) {
        projects.value = projects.value.map { if (it.id == id) it.copy(updatedAt = now) else it }
    }
    override suspend fun getAll(): List<Project> = projects.value
    override suspend fun upsertAll(list: List<Project>) = list.forEach { upsert(it) }
}

class ItemRepositoryImplTest {

    private val dao = FakeItemDao()
    private val linkDao = FakeItemLinkDao(dao)
    private val searchDao = FakeSearchDao()
    private var counter = 0
    private val projectDao = FakeProjectDao()
    private val repo = ItemRepositoryImpl(
        itemDao = dao,
        itemLinkDao = linkDao,
        searchDao = searchDao,
        projectDao = projectDao,
        reminderScheduler = NoOpReminderScheduler,
        clock = { 1_000L },
        idGenerator = { "id-${++counter}" },
    )

    @Test
    fun `capture lands a formless item in the inbox`() = runTest {
        val id = repo.capture("  یک فکر  ")

        val item = dao.observeInbox().first().single()
        assertEquals("id-1", id)
        assertEquals("یک فکر", item.content) // trimmed
        assertEquals("inbox", item.status)
        assertNull(item.type)                 // formless until triage
        assertEquals(1_000L, item.createdAt)
        assertEquals("quickAdd", item.capturedVia)
    }

    @Test
    fun `capture rejects empty content`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repo.capture("   ") }
        }
    }

    @Test
    fun `triage types an item, tags it, and moves it out of the inbox`() = runTest {
        val id = repo.capture("خرید مصالح")
        repo.triage(id, type = "task", projectId = "proj-1", tags = listOf("خانه", " خانه ", "فوری"))

        // No longer in the inbox.
        assertTrue(dao.observeInbox().first().isEmpty())

        val item = dao.getById(id)!!
        assertEquals("triaged", item.status)
        assertEquals("task", item.type)
        assertEquals("proj-1", item.projectId)
        // Now visible inside its project, derived by filtering.
        assertEquals(id, repo.observeByProject("proj-1").first().single().id)
        // Tags persisted as JSON, trimmed + de-duplicated.
        assertEquals(listOf("خانه", "فوری"), TagsCodec.decode(item.tags))
    }

    @Test
    fun `backlinks are queryable`() = runTest {
        val a = repo.capture("یادداشت الف")
        val b = repo.capture("یادداشت ب")
        repo.link(fromId = a, toId = b) // a → b

        val backlinksOfB = repo.observeBacklinks(b).first()
        assertEquals(listOf(a), backlinksOfB.map { it.id })

        val outgoingOfA = repo.observeOutgoing(a).first()
        assertEquals(listOf(b), outgoingOfA.map { it.id })
    }

    @Test
    fun `an item cannot link to itself`() = runTest {
        val a = repo.capture("الف")
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repo.link(a, a) }
        }
    }

    @Test
    fun `trash hides an item and restore brings it back to the inbox`() = runTest {
        val id = repo.capture("یک چیز")
        repo.trash(id)
        assertTrue(dao.observeInbox().first().isEmpty())
        assertEquals(id, repo.observeTrash().first().single().id)

        repo.restore(id)
        assertTrue(repo.observeTrash().first().isEmpty())
        assertEquals("inbox", dao.getById(id)!!.status)
    }

    @Test
    fun `restore returns a triaged item to its project`() = runTest {
        val id = repo.capture("کار خانه")
        repo.triage(id, type = "task", projectId = "p1")
        repo.trash(id)
        repo.restore(id)
        val item = dao.getById(id)!!
        assertEquals("triaged", item.status)
        assertEquals("p1", item.projectId)
    }
}
