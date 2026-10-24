package ani.dantotsu.parsers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap as AndroidBitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect as AndroidRect
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import ani.dantotsu.defaultHeaders
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.CookieJar
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaParser as KotatsuParser
import org.koitharu.kotatsu.parsers.bitmap.Bitmap as ParserBitmap
import org.koitharu.kotatsu.parsers.bitmap.Rect as ParserRect
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.webview.InterceptedRequest
import org.koitharu.kotatsu.parsers.webview.InterceptionConfig
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.io.use
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.atomic.AtomicBoolean

/** Bridges Kotatsu parsers to Kujaku's shared networking, preferences and Android bitmap APIs. */
class KotatsuMangaLoaderContext(context: Context) : MangaLoaderContext() {
    private val appContext = context.applicationContext
    private val networkHelper = Injekt.get<NetworkHelper>()

    override val httpClient = networkHelper.client
    override val cookieJar: CookieJar = networkHelper.cookieJar

    override fun getConfig(source: MangaSource): MangaSourceConfig =
        KotatsuSourceConfig(appContext, source)

    override fun getDefaultUserAgent(): String =
        runCatching { defaultHeaders["User-Agent"] }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: WebSettings.getDefaultUserAgent(appContext)

    override fun getPreferredLocales(): List<Locale> = listOf(Locale.getDefault())

    override fun encodeBase64(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)

    override fun decodeBase64(data: String): ByteArray = Base64.decode(data, Base64.DEFAULT)

    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun evaluateJs(script: String): String? = evaluateJs("", script, 10_000L)

    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? =
        withTimeout(timeout.coerceAtLeast(1L)) {
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine { continuation ->
                    val webView = WebView(appContext)
                    var evaluated = false
                    var disposed = false

                    fun dispose() {
                        if (disposed) return
                        disposed = true
                        runCatching { webView.stopLoading() }
                        runCatching { webView.destroy() }
                    }

                    fun complete(result: String?) {
                        if (continuation.isActive) continuation.resume(decodeJavascriptResult(result))
                        dispose()
                    }

                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            if (evaluated || !continuation.isActive) return
                            evaluated = true
                            view.evaluateJavascript(script) { result -> complete(result) }
                        }
                    }
                    continuation.invokeOnCancellation {
                        Handler(Looper.getMainLooper()).post { dispose() }
                    }
                    if (baseUrl.isBlank()) {
                        webView.loadDataWithBaseURL(
                            "https://kotatsu.invalid/",
                            "<html><body></body></html>",
                            "text/html",
                            "UTF-8",
                            null,
                        )
                    } else {
                        webView.loadUrl(baseUrl)
                    }
                }
            }
        }

    override suspend fun interceptWebViewRequests(
        url: String,
        interceptorScript: String,
        timeout: Long,
    ): List<InterceptedRequest> = interceptWebViewRequests(
        url,
        InterceptionConfig(timeoutMs = timeout, filterScript = interceptorScript),
    )

    override suspend fun interceptWebViewRequests(
        url: String,
        config: InterceptionConfig,
    ): List<InterceptedRequest> = captureWebViewRequests(url, config)

    override suspend fun captureWebViewUrls(
        pageUrl: String,
        urlPattern: Regex,
        timeout: Long,
    ): List<String> = captureWebViewRequests(
        pageUrl,
        InterceptionConfig(timeoutMs = timeout, urlPattern = urlPattern),
    ).map { it.url }

    private suspend fun captureWebViewRequests(
        pageUrl: String,
        config: InterceptionConfig,
    ): List<InterceptedRequest> = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val captured = mutableListOf<InterceptedRequest>()
            val webView = WebView(appContext)
            val handler = Handler(Looper.getMainLooper())
            val disposed = AtomicBoolean(false)
            var hardTimeout: Runnable? = null
            var idleFinish: Runnable? = null
            var pageScriptStarted = false

            fun cleanup() {
                hardTimeout?.let(handler::removeCallbacks)
                idleFinish?.let(handler::removeCallbacks)
                runCatching { webView.stopLoading() }
                runCatching { webView.destroy() }
            }

            fun dispose() {
                if (disposed.compareAndSet(false, true)) cleanup()
            }

            fun complete() {
                if (!disposed.compareAndSet(false, true)) return
                val result = synchronized(captured) { captured.toList() }
                cleanup()
                if (continuation.isActive) continuation.resume(result)
            }

            fun scheduleIdleFinish(delayMs: Long = 750L) {
                idleFinish?.let(handler::removeCallbacks)
                idleFinish = Runnable { complete() }
                handler.postDelayed(idleFinish!!, delayMs)
            }

            hardTimeout = Runnable { complete() }
            idleFinish = Runnable { complete() }
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.userAgentString = getDefaultUserAgent()
            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): android.webkit.WebResourceResponse? {
                    val intercepted = InterceptedRequest(
                        url = request.url.toString(),
                        method = request.method,
                        headers = request.requestHeaders,
                        timestamp = System.currentTimeMillis(),
                    )
                    val patternMatches = config.urlPattern?.containsMatchIn(intercepted.url) != false
                    val scriptMatches = matchesInterceptionFilter(config.filterScript, intercepted.url)
                    if (patternMatches && scriptMatches && !disposed.get()) {
                        val reachedLimit = synchronized(captured) {
                            if (captured.size >= config.maxRequests.coerceAtLeast(1)) {
                                true
                            } else {
                                captured.add(intercepted)
                                captured.size >= config.maxRequests.coerceAtLeast(1)
                            }
                        }
                        handler.post {
                            if (reachedLimit) complete() else scheduleIdleFinish()
                        }
                    }
                    return null
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (!config.pageScript.isNullOrBlank() && !pageScriptStarted) {
                        pageScriptStarted = true
                        view.evaluateJavascript(config.pageScript) { scheduleIdleFinish() }
                    } else {
                        scheduleIdleFinish()
                    }
                }
            }
            continuation.invokeOnCancellation { handler.post { dispose() } }
            handler.postDelayed(hardTimeout!!, config.timeoutMs.coerceAtLeast(1L))
            runCatching { webView.loadUrl(pageUrl) }.onFailure { complete() }
        }
    }

    private fun matchesInterceptionFilter(script: String?, requestUrl: String): Boolean {
        if (script.isNullOrBlank()) return true
        val returnIndex = script.lastIndexOf("return")
        if (returnIndex < 0) return true
        val expression = script.substring(returnIndex + "return".length).substringBefore(";").trim()
        if (expression.isEmpty() || expression == "true") return true
        if (expression == "false") return false
        return expression.split("||").any { clause ->
            clause.trim().trim('(', ')').split("&&").all { rawTerm ->
                val term = rawTerm.trim().trim('(', ')').trim()
                when {
                    term == "true" -> true
                    term == "false" -> false
                    term.startsWith("url.includes(") && term.endsWith(")") -> {
                        val needle = term.substringAfter("url.includes(")
                            .substringBeforeLast(")")
                            .trim()
                            .trim { it == 39.toChar() || it == 34.toChar() }
                        requestUrl.contains(needle)
                    }
                    else -> false
                }
            }
        }
    }

    override fun requestBrowserAction(parser: KotatsuParser, url: String): Nothing =
        throw UnsupportedOperationException(
            "Kotatsu parser " + parser.source.name + " requires a browser action for " + url,
        )

    override fun redrawImageResponse(
        response: Response,
        redraw: (image: ParserBitmap) -> ParserBitmap,
    ): Response {
        val originalBody = response.body ?: return response
        val contentType = originalBody.contentType()
        val originalBytes = originalBody.bytes()
        val decoded = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
            ?: return response.newBuilder().body(originalBytes.toResponseBody(contentType)).build()
        val input = KotatsuBitmap.create(decoded)
        val output = try {
            redraw(input) as KotatsuBitmap
        } catch (error: Exception) {
            input.close()
            return response.newBuilder().body(originalBytes.toResponseBody(contentType)).build()
        }
        val outputBytes = try {
            ByteArrayOutputStream().use { stream ->
                output.compressTo(stream)
                stream.toByteArray()
            }
        } finally {
            output.close()
            if (output !== input) input.close()
        }
        return response.newBuilder()
            .body(outputBytes.toResponseBody("image/png".toMediaType()))
            .build()
    }

    override fun createBitmap(width: Int, height: Int): ParserBitmap =
        KotatsuBitmap.create(AndroidBitmap.createBitmap(width, height, AndroidBitmap.Config.ARGB_8888))

    private fun decodeJavascriptResult(result: String?): String? {
        if (result == null || result == "null" || result == "undefined") return null
        return runCatching { JsonParser.parseString(result).asString }.getOrDefault(result)
    }

    private class KotatsuSourceConfig(
        context: Context,
        source: MangaSource,
    ) : MangaSourceConfig {
        private val preferences = context.getSharedPreferences(
            "kotatsu_source_" + source.name,
            Context.MODE_PRIVATE,
        )

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: ConfigKey<T>): T {
            val default = key.defaultValue
            val value: Any? = when (default) {
                is String -> preferences.getString(key.key, default)
                is Boolean -> preferences.getBoolean(key.key, default)
                null -> preferences.getString(key.key, null)
                else -> default
            }
            return (value ?: default) as T
        }
    }
}

private class KotatsuBitmap private constructor(
    private val androidBitmap: AndroidBitmap,
) : ParserBitmap, AutoCloseable {
    private val canvas by lazy { Canvas(androidBitmap) }

    override val width: Int get() = androidBitmap.width
    override val height: Int get() = androidBitmap.height

    override fun drawBitmap(sourceBitmap: ParserBitmap, src: ParserRect, dst: ParserRect) {
        val source = (sourceBitmap as KotatsuBitmap).androidBitmap
        canvas.drawBitmap(
            source,
            AndroidRect(src.left, src.top, src.right, src.bottom),
            AndroidRect(dst.left, dst.top, dst.right, dst.bottom),
            null,
        )
    }

    override fun close() {
        if (!androidBitmap.isRecycled) androidBitmap.recycle()
    }

    fun compressTo(output: java.io.OutputStream) {
        androidBitmap.compress(AndroidBitmap.CompressFormat.PNG, 100, output)
    }

    companion object {
        fun create(bitmap: AndroidBitmap): KotatsuBitmap = KotatsuBitmap(
            if (bitmap.isMutable) bitmap else bitmap.copy(AndroidBitmap.Config.ARGB_8888, true),
        )
    }
}