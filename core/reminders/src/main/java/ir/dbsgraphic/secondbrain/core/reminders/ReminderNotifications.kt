package ir.dbsgraphic.secondbrain.core.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

internal object ReminderNotifications {

    const val CHANNEL_ID = "reminders"

    fun show(context: Context, itemId: String, content: String) {
        ensureChannel(context)

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = launch?.let {
            PendingIntent.getActivity(
                context,
                itemId.hashCode(),
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("یادآوری")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply { if (pendingIntent != null) setContentIntent(pendingIntent) }
            .build()

        // notify() no-ops if POST_NOTIFICATIONS isn't granted (API 33+); guard anyway.
        runCatching {
            NotificationManagerCompat.from(context).notify(itemId.hashCode(), notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "یادآوری‌ها",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "یادآوری‌های مغز دوم" }
        manager.createNotificationChannel(channel)
    }
}
