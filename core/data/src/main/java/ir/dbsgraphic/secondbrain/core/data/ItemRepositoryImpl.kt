package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.PersianNormalizer
import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemLinkDao
import ir.dbsgraphic.secondbrain.core.database.dao.SearchDao
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.ItemLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val itemLinkDao: ItemLinkDao,
    private val searchDao: SearchDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : ItemRepository {

    override fun observeInbox(): Flow<List<Item>> = itemDao.observeInbox()

    override fun observeInboxCount(): Flow<Int> = itemDao.observeInboxCount()

    override fun observeById(id: String): Flow<Item?> = itemDao.observeById(id)

    override fun observeTimeline(): Flow<List<Item>> = itemDao.observeTimeline()

    override fun search(rawQuery: String): Flow<List<Item>> {
        val match = buildMatchQuery(rawQuery) ?: return flowOf(emptyList())
        return searchDao.search(match)
    }

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

    override suspend fun captureShared(content: String, blobRef: String?, contentType: String): String {
        val text = content.trim().ifEmpty {
            when (contentType) {
                "image" -> "تصویر"
                "file" -> "فایل"
                "link" -> "لینک"
                else -> "یادداشت"
            }
        }
        val now = clock.now()
        val item = Item(
            id = idGenerator.newId(),
            createdAt = now,
            updatedAt = now,
            content = text,
            blobRef = blobRef,
            contentType = contentType,
            capturedVia = "share",
            status = "inbox",
            type = null,
        )
        itemDao.upsert(item)
        return item.id
    }

    override suspend fun captureBlob(
        blobRef: String,
        contentType: String,
        capturedVia: String,
        content: String,
    ): String {
        val text = content.trim().ifEmpty {
            when (contentType) {
                "image" -> "تصویر"
                "voice" -> "یادداشت صوتی"
                "file" -> "فایل"
                else -> "یادداشت"
            }
        }
        val now = clock.now()
        val item = Item(
            id = idGenerator.newId(),
            createdAt = now,
            updatedAt = now,
            content = text,
            blobRef = blobRef,
            contentType = contentType,
            capturedVia = capturedVia,
            status = "inbox",
            type = null,
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

    /**
     * Turn raw user text into an FTS5 MATCH string. Normalized the same way the
     * index is, split into clean tokens (letters/digits only), each made a
     * **bareword prefix** term (`token*`) so results appear as you type. FTS5's
     * `*` prefix operator applies to barewords — not quoted phrases — so we must
     * not quote. Returns null when there's nothing to search.
     */
    private fun buildMatchQuery(raw: String): String? {
        val normalized = PersianNormalizer.normalize(raw)
        val tokens = normalized.split(Regex("\\s+"))
            .map { token -> token.filter { it.isLetterOrDigit() } }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }
}
