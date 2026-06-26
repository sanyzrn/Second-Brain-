package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.database.dao.HabitCheckinDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemLinkDao
import ir.dbsgraphic.secondbrain.core.database.dao.ProjectDao
import ir.dbsgraphic.secondbrain.core.database.entity.HabitCheckin
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.ItemLink
import ir.dbsgraphic.secondbrain.core.database.entity.Project
import ir.dbsgraphic.secondbrain.core.security.KeystoreCipher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class BackupData(
    val version: Int = 1,
    val items: List<Item> = emptyList(),
    val projects: List<Project> = emptyList(),
    val links: List<ItemLink> = emptyList(),
    val habitCheckins: List<HabitCheckin> = emptyList(),
)

/**
 * Encrypted, whole-database export and import — the user owns their data and can
 * take it anytime (Constitution §9). The bytes are AES-256-GCM encrypted with a
 * Keystore key, so the backup is private and device-bound (§11).
 */
class BackupManager @Inject constructor(
    private val itemDao: ItemDao,
    private val projectDao: ProjectDao,
    private val itemLinkDao: ItemLinkDao,
    private val habitCheckinDao: HabitCheckinDao,
    private val cipher: KeystoreCipher,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    /** Returns the encrypted backup bytes for the caller to write to a SAF uri. */
    suspend fun exportAll(): ByteArray {
        val data = BackupData(
            items = itemDao.getAll(),
            projects = projectDao.getAll(),
            links = itemLinkDao.getAll(),
            habitCheckins = habitCheckinDao.getAll(),
        )
        val plaintext = json.encodeToString(BackupData.serializer(), data).toByteArray(Charsets.UTF_8)
        return cipher.encrypt(plaintext)
    }

    /** Restores from encrypted bytes. Merges (upserts) — nothing is destroyed. */
    suspend fun importAll(bytes: ByteArray) {
        val plaintext = cipher.decrypt(bytes).toString(Charsets.UTF_8)
        val data = json.decodeFromString(BackupData.serializer(), plaintext)
        projectDao.upsertAll(data.projects)
        itemDao.upsertAll(data.items)
        itemLinkDao.insertAll(data.links)
        habitCheckinDao.upsertAll(data.habitCheckins)
    }
}
