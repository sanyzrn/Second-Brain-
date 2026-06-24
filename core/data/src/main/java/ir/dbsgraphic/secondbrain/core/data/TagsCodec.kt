package ir.dbsgraphic.secondbrain.core.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Tags are stored on the Item as a JSON array string (the `tags` column).
 * Uses kotlinx-serialization's built-in serializers — no codegen plugin, so it
 * runs in plain JVM unit tests too.
 */
object TagsCodec {
    private val serializer = ListSerializer(String.serializer())

    fun encode(tags: List<String>): String {
        val clean = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return Json.encodeToString(serializer, clean)
    }

    fun decode(json: String): List<String> =
        runCatching { Json.decodeFromString(serializer, json) }.getOrDefault(emptyList())
}
