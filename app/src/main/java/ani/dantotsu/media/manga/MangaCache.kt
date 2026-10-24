package ani.dantotsu.media.manga

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.LruCache
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileOutputStream

class ImageData private constructor(
    val page: Page,
    val source: HttpSource?,
    private val kotatsuRequest: KotatsuRequest?,
) {
    private data class KotatsuRequest(val client: OkHttpClient, val request: Request)

    constructor(page: Page, source: HttpSource) : this(page, source, null)

    suspend fun fetchAndProcessImage(
        page: Page = this.page,
        httpSource: HttpSource? = source,
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val response = kotatsuRequest?.let { request ->
                request.client.newCall(request.request).execute()
            } ?: run {
                val source = httpSource ?: return@withContext null
                source.getImage(page)
            }
            response.use { result ->
                if (!result.isSuccessful) {
                    Logger.log("Image request failed: " + result.code + " " + result.message)
                    return@withContext null
                }
                val body = result.body ?: return@withContext null
                BitmapFactory.decodeStream(body.byteStream())
            }
        } catch (e: Exception) {
            Logger.log("An error occurred: " + e.message)
            snackString("An error occurred: " + e.message)
            null
        }
    }

    companion object {
        fun forKotatsu(
            page: Page,
            imageUrl: String,
            headers: Headers,
            client: OkHttpClient,
        ): ImageData {
            val request = Request.Builder().url(imageUrl).headers(headers).build()
            return ImageData(page, null, KotatsuRequest(client, request))
        }
    }
}
fun saveImage(
    bitmap: Bitmap,
    contentResolver: ContentResolver,
    filename: String,
    format: Bitmap.CompressFormat,
    quality: Int
) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/${format.name.lowercase()}")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/Dantotsu/Manga"
                )
            }

            val uri: Uri? =
                contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                contentResolver.openOutputStream(it)?.use { os ->
                    bitmap.compress(format, quality, os)
                }
            }
        } else {
            val directory =
                File("${Environment.getExternalStorageDirectory()}${File.separator}Dantotsu${File.separator}Manga")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, filename)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(format, quality, outputStream)
            }
        }
    } catch (e: Exception) {
        // Handle exception here
        println("Exception while saving image: ${e.message}")
    }
}

class MangaCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024 / 2).toInt()
    private val cache = LruCache<String, ImageData>(maxMemory)

    @Synchronized
    fun put(key: String, imageDate: ImageData) {
        cache.put(key, imageDate)
    }

    @Synchronized
    fun get(key: String): ImageData? = cache.get(key)

    @Synchronized
    fun remove(key: String) {
        cache.remove(key)
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
    }

    fun size(): Int = cache.size()


}
