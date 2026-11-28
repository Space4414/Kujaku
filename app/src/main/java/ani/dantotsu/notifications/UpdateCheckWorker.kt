package ani.dantotsu.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.Logger
import org.json.JSONArray

class UpdateCheckWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!PrefManager.getVal<Boolean>(PrefName.CheckUpdate)) return Result.success()
        return try {
            val prefix = channelPrefix()
            val releases = JSONArray(
                client.get("https://api.github.com/repos/Space4414/Kujaku/releases?per_page=50").text
            )
            val candidates = mutableListOf<Release>()
            for (index in 0 until releases.length()) {
                val item = releases.optJSONObject(index) ?: continue
                val tag = item.optString("tag_name")
                if (tag.startsWith(prefix) && !item.optBoolean("draft", true)) {
                    candidates += Release(tag, item.optString("name", tag), item.optString("html_url"))
                }
            }
            val latest = candidates.maxByOrNull { versionParts(versionFromTag(it.tag)).toVersionKey() }
                ?: return Result.success()
            val version = versionFromTag(latest.tag)
            val current = BuildConfig.VERSION_NAME.substringBefore(" -").trim()
            if (!isNewer(version, current)) {
                return Result.success()
            }

            val notifiedKey = "update_center_notified_${BuildConfig.BUILD_TYPE}"
            if (PrefManager.getCustomVal(notifiedKey, "") == latest.tag) return Result.success()
            if (!notificationsAllowed()) return Result.success()

            createNotificationChannel()
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(latest.url))
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 4102, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_round_new_releases_24)
                .setContentTitle("Kujaku update available")
                .setContentText("Version $version is available (you have $current)")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Version $version is available (you have $current).\nTap to view the release and choose whether to download it."
                    )
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            PrefManager.setCustomVal(notifiedKey, latest.tag)
            Result.success()
        } catch (error: Exception) {
            Logger.log("Update check failed: $error")
            Result.retry()
        }
    }

    private fun channelPrefix(): String = when (BuildConfig.BUILD_TYPE) {
        "alpha" -> "alpha-v"
        "debug" -> "beta-v"
        else -> "v"
    }

    private fun versionFromTag(tag: String) = tag.removePrefix(channelPrefix())
    private fun versionParts(version: String) =
        version.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }

    private fun List<Int>.toVersionKey(): Long =
        fold(0L) { total, part -> total * 1000L + part }

    private fun isNewer(candidate: String, current: String): Boolean {
        val left = versionParts(candidate)
        val right = versionParts(current)
        for (index in 0 until maxOf(left.size, right.size)) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun notificationsAllowed(): Boolean {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Notifications when a new Kujaku version is available"
                }
            )
        }
    }

    private data class Release(val tag: String, val name: String, val url: String)

    companion object {
        const val WORK_NAME = "kujaku_app_update_check"
        private const val CHANNEL_ID = "kujaku_app_updates"
        private const val NOTIFICATION_ID = 4102
    }
}
