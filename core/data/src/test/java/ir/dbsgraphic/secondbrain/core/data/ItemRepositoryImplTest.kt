package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemLinkDao
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.ItemLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    override fun observeInboxCount(): Flow<Int> =
        items.map { list -> list.count { it.status == "inbox" } }
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

class ItemRepositoryImplTest {

    private val dao = FakeItemDao()
    private val linkDao = FakeItemLinkDao(dao)
    private var counter = 0
    private val repo = ItemRepositoryImpl(
        itemDao = dao,
        itemLinkDao = linkDao,
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
}
