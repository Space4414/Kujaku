package ani.dantotsu.parsers

import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import eu.kanade.tachiyomi.network.NetworkHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class KotatsuVerificationComplete : RuntimeException()

class KotatsuVerificationActivity : Activity() {
    private var completed = false
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val url = intent.getStringExtra("url")
        if (url == null || !url.startsWith("https://")) {
            finish()
            return
        }
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        val done = Button(this).apply {
            text = "Verification complete"
            setOnClickListener {
                val manager = CookieManager.getInstance()
                manager.flush()
                val target = url.toHttpUrl()
                val cookies = manager.getCookie(url).orEmpty().split(";")
                    .mapNotNull { Cookie.parse(target, it.trim()) }
                if (cookies.isEmpty()) return@setOnClickListener
                Injekt.get<NetworkHelper>().cookieJar.saveFromResponse(target, cookies)
                completed = true
                finish()
            }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(done)
        })
        web.loadUrl(url)
    }
    override fun onDestroy() {
        pending.getAndSet(null)?.invoke(completed)
        super.onDestroy()
    }
    companion object {
        val pending = AtomicReference<((Boolean) -> Unit)?>(null)
    }
}