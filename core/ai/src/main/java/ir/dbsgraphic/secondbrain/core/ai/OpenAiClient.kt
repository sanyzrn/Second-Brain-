package ir.dbsgraphic.secondbrain.core.ai

import android.util.Base64
import ir.dbsgraphic.secondbrain.core.data.AiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal client for any OpenAI-compatible endpoint (Chat Completions + Audio
 * Transcriptions). No SDK — plain HTTPS + JSON — so the app pulls in no cloud
 * dependency. Every call is best-effort: any failure returns null so AI never
 * breaks the app.
 */
class OpenAiClient {

    private val json = Json { ignoreUnknownKeys = true }

    /** Chat completion. [imageDataUrl] adds a vision part (for OCR). */
    suspend fun chat(
        config: AiConfig,
        system: String,
        user: String,
        imageDataUrl: String? = null,
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val body = buildJsonObject {
                put("model", config.chatModel)
                put("temperature", 0.2)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "system")
                        put("content", system)
                    }
                    addJsonObject {
                        put("role", "user")
                        if (imageDataUrl == null) {
                            put("content", user)
                        } else {
                            putJsonArray("content") {
                                addJsonObject {
                                    put("type", "text")
                                    put("text", user)
                                }
                                addJsonObject {
                                    put("type", "image_url")
                                    putJsonObject("image_url") { put("url", imageDataUrl) }
                                }
                            }
                        }
                    }
                }
            }
            val response = postJson("${config.baseUrl.trimEnd('/')}/chat/completions", config.apiKey, body.toString())
                ?: return@runCatching null
            json.parseToJsonElement(response)
                .jsonObject["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")
                ?.jsonPrimitive?.content?.trim()
        }.getOrNull()
    }

    /** Audio transcription via multipart/form-data. */
    suspend fun transcribe(config: AiConfig, audioPath: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(audioPath)
            if (!file.exists()) return@runCatching null
            val boundary = "----secondbrain${System.currentTimeMillis()}"
            val url = URL("${config.baseUrl.trimEnd('/')}/audio/transcriptions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            conn.outputStream.use { out ->
                fun writeField(name: String, value: String) {
                    out.write("--$boundary\r\n".toByteArray())
                    out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
                    out.write("$value\r\n".toByteArray())
                }
                writeField("model", config.transcribeModel)
                out.write("--$boundary\r\n".toByteArray())
                out.write(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"audio.m4a\"\r\n".toByteArray(),
                )
                out.write("Content-Type: audio/mp4\r\n\r\n".toByteArray())
                out.write(file.readBytes())
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: return@runCatching null
            if (code !in 200..299) return@runCatching null
            json.parseToJsonElement(response).jsonObject["text"]?.jsonPrimitive?.content?.trim()
        }.getOrNull()
    }

    fun imageToDataUrl(imagePath: String): String? = runCatching {
        val bytes = File(imagePath).readBytes()
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()

    private fun postJson(urlStr: String, apiKey: String, body: String): String? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }
        if (code in 200..299) text else null
    }.getOrNull()
}
