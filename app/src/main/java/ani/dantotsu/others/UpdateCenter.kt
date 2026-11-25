package ani.dantotsu.others

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import ani.dantotsu.BuildConfig
import ani.dantotsu.client
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

object UpdateCenter {
    private const val REPOSITORY = "Space4414/Kujaku"
    private const val RELEASES_URL = "https://api.github.com/repos/$REPOSITORY/releases?per_page=50"

    private data class Release(val tag: String, val name: String, val body: String, val url: String)

    private fun channelPrefix(): String = when (BuildConfig.BUILD_TYPE) {
        "alpha" -> "alpha-v"
        "debug" -> "beta-v"
        else -> "v"
    }

    private fun currentVersion(): String = BuildConfig.VERSION_NAME.substringBefore(" -").trim()
    private fun versionFromTag(tag: String): String = tag.removePrefix(channelPrefix())
    private fun parseVersion(version: String): List<Int> =
        version.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }

    private fun isNewer(candidate: String, current: String): Boolean {
        val left = parseVersion(candidate)
        val right = parseVersion(current)
        for (index in 0 until maxOf(left.size, right.size)) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private suspend fun fetchChannelReleases(): List<Release> = withContext(Dispatchers.IO) {
        val releases = JSONArray(client.get(RELEASES_URL).text)
        buildList {
            for (index in 0 until releases.length()) {
                val item = releases.optJSONObject(index) ?: continue
                val tag = item.optString("tag_name")
                if (tag.startsWith(channelPrefix()) && !item.optBoolean("draft", true)) {
                    add(Release(tag, item.optString("name", tag), item.optString("body"), item.optString("html_url")))
                }
            }
        }.sortedByDescending { release ->
            parseVersion(versionFromTag(release.tag)).fold(0L) { total, part -> total * 1000L + part }
        }
    }

    fun checkForUpdates(activity: Activity) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching { fetchChannelReleases().firstOrNull() }
            withContext(Dispatchers.Main) {
                if (activity.isFinishing || activity.isDestroyed) return@withContext
                val release = result.getOrNull()
                if (release == null) {
                    activity.toast("Couldn't check for Kujaku updates. Please try again.")
                    return@withContext
                }
                val version = versionFromTag(release.tag)
                if (!isNewer(version, currentVersion())) {
                    activity.toast("You're using the latest Kujaku version for this channel.")
                    return@withContext
                }
                AlertDialog.Builder(activity)
                    .setTitle(release.name.ifBlank { "Kujaku $version" })
                    .setMessage(release.body.ifBlank { "A new update is available." })
                    .setNegativeButton("Later", null)
                    .setPositiveButton("View update") { _, _ -> activity.openLinkInBrowser(release.url) }
                    .show()
            }
        }
    }

    fun showChangelog(activity: Activity) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching { fetchChannelReleases().take(8) }
            withContext(Dispatchers.Main) {
                if (activity.isFinishing || activity.isDestroyed) return@withContext
                val releases = result.getOrNull()
                if (releases.isNullOrEmpty()) {
                    activity.toast("Couldn't load the Kujaku changelog.")
                    return@withContext
                }
                val message = releases.joinToString("\n\n") { release ->
                    val heading = release.name.ifBlank { release.tag }
                    "$heading\n\${release.body.ifBlank { "No release notes provided." }}"
                }
                AlertDialog.Builder(activity)
                    .setTitle("Kujaku Changelog")
                    .setMessage(message)
                    .setPositiveButton("Open releases") { _, _ ->
                        activity.openLinkInBrowser("https://github.com/$REPOSITORY/releases")
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }
}
