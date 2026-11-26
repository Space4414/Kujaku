package ani.dantotsu.connections.discord

import android.annotation.SuppressLint
import android.app.Application.getProcessName
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.R
import ani.dantotsu.connections.discord.Discord.saveToken
import ani.dantotsu.startMainActivity
import ani.dantotsu.themes.ThemeManager

class Login : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var done = false
    private var polls = 0
    private val poller = object : Runnable {
        override fun run() {
            if (done || isFinishing || isDestroyed) return
            webView?.let { checkToken(it) }
            if (++polls < MAX_POLLS) handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }
        setContentView(R.layout.activity_discord)
        val wv = findViewById<WebView>(R.id.discordWebview)
        webView = wv
        wv.settings.apply {
            javaScriptEnabled = true
            databaseEnabled = true
            domStorageEnabled = true
            userAgentString = USER_AGENT
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.let { checkToken(it) }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.let { checkToken(it) }
            }
        }
        wv.loadUrl("https://discord.com/login")
        handler.postDelayed(poller, POLL_INTERVAL_MS)
    }
    private fun checkToken(view: WebView) {
        if (done) return
        view.evaluateJavascript(TOKEN_JS) { result ->
            if (done) return@evaluateJavascript
            val token = result?.replace("\\\"", "")?.trim('"')
                ?.takeUnless { it.isBlank() || it == "null" || it == "undefined" }
            if (token != null) login(token)
        }
    }
    private fun login(token: String) {
        if (done) return
        done = true
        handler.removeCallbacks(poller)
        saveToken(token)
        Toast.makeText(this, "Logged in successfully", Toast.LENGTH_SHORT).show()
        finish()
        startMainActivity(this@Login)
    }
    override fun onDestroy() {
        handler.removeCallbacks(poller)
        webView?.apply {
            stopLoading()
            webViewClient = WebViewClient()
            destroy()
        }
        webView = null
        super.onDestroy()
    }
    private companion object {
        const val POLL_INTERVAL_MS = 1500L
        const val MAX_POLLS = 400
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; SM-S921U) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"
        val TOKEN_JS = """
            (function() {
                try {
                    var t = window.localStorage.getItem('token');
                    if (!t) {
                        var f = document.createElement('iframe');
                        f.style.display = 'none';
                        document.body.appendChild(f);
                        t = f.contentWindow.localStorage.getItem('token');
                        document.body.removeChild(f);
                    }
                    return t ? String(t).replace(/^"|"$/g, '') : null;
                } catch (e) { return null; }
            })()
        """.trimIndent()
    }
}
