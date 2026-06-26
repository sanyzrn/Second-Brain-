package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.PersianNormalizer
import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemLinkDao
import ir.dbsgraphic.secondbrain.core.database.dao.ProjectDao
import ir.dbsgraphic.secondbrain.core.database.dao.SearchDao
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.ItemLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val itemLinkDao: ItemLinkDao,
    private val searchDao: SearchDao,
    private val projectDao: ProjectDao,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : ItemRepository {

    override fun observeInbox(): Flow<List<Item>> = itemDao.observeInbox()

    override fun observeInboxCount(): Flow<Int> = itemDao.observeInboxCount()

    override suspend fun updateContent(id: String, content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        val item = itemDao.getById(id) ?: return
        itemDao.update(item.copy(content = trimmed, updatedAt = clock.now()))
    }

    override fun observeReminders(): Flow<List<Item>> = itemDao.observeReminders()

    override suspend fun setReminder(id: String, whenMillis: Long?) {
        val item = itemDao.getById(id) ?: return
        itemDao.update(item.copy(reminderAt = whenMillis, updatedAt = clock.now()))
        if (whenMillis != null && whenMillis > clock.now()) {
            reminderScheduler.schedule(id, item.content, whenMillis)
        } else {
            reminderScheduler.cancel(id)
        }
    }

    override fun observeTrash(): Flow<List<Item>> = itemDao.observeTrashed()

    override suspend fun trash(id: String) {
        val item = itemDao.getById(id) ?: return
        reminderScheduler.cancel(id)
        itemDao.update(item.copy(status = "trashed", updatedAt = clock.now()))
    }

    override suspend fun restore(id: String) {
        val item = itemDao.getById(id) ?: return
        val restored = if (item.type != null) "triaged" else "inbox"
        itemDao.update(item.copy(status = restored, updatedAt = clock.now()))
    }

    override suspend fun deleteForever(id: String) {
        val item = itemDao.getById(id) ?: return
        reminderScheduler.cancel(id)
        deleteBlob(item.blobRef)
        itemDao.deleteById(id)
    }

    override suspend fun emptyTrash() {
        itemDao.observeTrashed().first().forEach {
            reminderScheduler.cancel(it.id)
            deleteBlob(it.blobRef)
        }
        itemDao.deleteAllTrashed()
    }

    private fun deleteBlob(blobRef: String?) {
        if (blobRef.isNullOrBlank()) return
        runCatching { java.io.File(blobRef).delete() }
    }

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
        val now = clock.now()
        itemDao.update(
            item.copy(
                type = type,
                status = "triaged",
                projectId = projectId,
                tags = TagsCodec.encode(tags),
                updatedAt = now,
            ),
        )
        // Keep the Projects list ordered by activity (§ review fix).
        if (projectId != null) projectDao.touch(projectId, now)
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
