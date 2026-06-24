package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemLinkDao
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.ItemLink
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val itemLinkDao: ItemLinkDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : ItemRepository {

    override fun observeInbox(): Flow<List<Item>> = itemDao.observeInbox()

    override fun observeInboxCount(): Flow<Int> = itemDao.observeInboxCount()

    override fun observeById(id: String): Flow<Item?> = itemDao.observeById(id)

    override fun observeByProject(projectId: String): Flow<List<Item>> =
        itemDao.observeByProject(projectId)

    override suspend fun capture(content: String): String {
        val trimmed = content.trim()
        require(trimmed.isNotEmpty()) { "Cannot capture empty content" }

        val now = clock.now()
        val item = Item(
            id = idGenerator.newId(),
            createdAt = now,
            updatedAt = now,
            content = trimmed,
            status = "inbox",
            type = null,
            contentType = "text",
            capturedVia = "quickAdd",
        )
        itemDao.upsert(item)
        return item.id
    }

    override suspend fun triage(
        itemId: String,
        type: String,
        projectId: String?,
        tags: List<String>,
    ) {
        val item = itemDao.getById(itemId) ?: return
        itemDao.update(
            item.copy(
                type = type,
                status = "triaged",
                projectId = projectId,
                tags = TagsCodec.encode(tags),
                updatedAt = clock.now(),
            ),
        )
    }

    override fun observeBacklinks(itemId: String): Flow<List<Item>> =
        itemLinkDao.observeBacklinks(itemId)

    override fun observeOutgoing(itemId: String): Flow<List<Item>> =
        itemLinkDao.observeOutgoing(itemId)

    override suspend fun link(fromId: String, toId: String, kind: String) {
        require(fromId != toId) { "An item cannot link to itself" }
        itemLinkDao.insert(ItemLink(fromId = fromId, toId = toId, kind = kind, createdAt = clock.now()))
    }

    override suspend fun unlink(fromId: String, toId: String, kind: String) {
        itemLinkDao.delete(fromId, toId, kind)
    }
}
