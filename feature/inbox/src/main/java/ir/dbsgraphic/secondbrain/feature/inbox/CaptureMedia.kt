package ir.dbsgraphic.secondbrain.feature.inbox

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID

/** Creates a unique file under the app-private blobs dir (offline, §10/§11). */
internal fun createBlobFile(context: Context, extension: String): File {
    val dir = File(context.filesDir, "blobs").apply { mkdirs() }
    return File(dir, "${UUID.randomUUID()}.$extension")
}

internal fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

/** Thin wrapper over MediaRecorder for quick voice capture. */
internal class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    fun start(path: String): Boolean = try {
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setOutputFile(path)
        r.prepare()
        r.start()
        recorder = r
        true
    } catch (e: Exception) {
        release()
        false
    }

    /** Stops and returns true if recording finished cleanly. */
    fun stop(): Boolean = try {
        recorder?.stop()
        true
    } catch (e: Exception) {
        false
    } finally {
        release()
    }

    fun release() {
        recorder?.runCatching { reset(); release() }
        recorder = null
    }
}
