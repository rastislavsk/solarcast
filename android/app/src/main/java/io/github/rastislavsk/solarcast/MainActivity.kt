package io.github.rastislavsk.solarcast

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.toColorInt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Native shell around the standalone SolarCast page.
 *
 * The page is served out of the APK over https://appassets.androidplatform.net
 * instead of file://, so it runs in an ordinary secure origin: localStorage
 * survives updates and the Open-Meteo calls are plain CORS requests.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private lateinit var backCallback: OnBackPressedCallback

    private var pageBackground: Int = DEFAULT_BACKGROUND

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
            setBackgroundColor(pageBackground)
        }
        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
            setBackgroundColor(Color.TRANSPARENT)
        }
        root.addView(webView)
        setContentView(root)

        applyInsets()
        configureWebView()
        listenForThemeChanges()
        wireBackNavigation()

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(START_URL)
        }
    }

    /**
     * Edge to edge is mandatory from API 35 on, so the bars no longer reserve
     * space for us. The page knows nothing about cutouts, so the shell insets it.
     */
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            view.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val loader = WebViewAssetLoader.Builder()
            .setDomain(ASSET_HOST)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            // Location, strings and inverter settings all live in localStorage.
            domStorageEnabled = true
            // Nothing is ever read from disk or from another app's provider.
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            // The page is responsive already; the zoom widgets only fight it.
            builtInZoomControls = false
            displayZoomControls = false
        }

        webView.isVerticalScrollBarEnabled = true
        webView.overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS

        webView.webViewClient = object : WebViewClientCompat() {

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url
                if (url.host == ASSET_HOST) return false
                // Anything off our own origin, such as the Open-Meteo docs link,
                // belongs in the browser rather than inside the app.
                openExternally(url)
                return true
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                backCallback.isEnabled = view.canGoBack()
            }

            override fun onPageFinished(view: WebView, url: String) {
                backCallback.isEnabled = view.canGoBack()
                injectThemeReporter(view)
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                // Rebuilding the activity beats letting the renderer take the
                // whole process down with it.
                root.removeView(view)
                view.destroy()
                recreate()
                return true
            }
        }
    }

    /**
     * The page owns its palette: three colour schemes, each in light and dark.
     * It reports whatever it settled on so the window behind it and the system
     * bar icons match, instead of flashing a hard-coded dark rectangle.
     */
    private fun listenForThemeChanges() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        WebViewCompat.addWebMessageListener(
            webView,
            HOST_OBJECT,
            setOf("https://$ASSET_HOST"),
            object : WebViewCompat.WebMessageListener {
                override fun onPostMessage(
                    view: WebView,
                    message: WebMessageCompat,
                    sourceOrigin: Uri,
                    isMainFrame: Boolean,
                    replyProxy: JavaScriptReplyProxy
                ) {
                    if (!isMainFrame) return
                    val parts = (message.data ?: return).split('|')
                    applyPageTheme(parts.getOrNull(0).orEmpty(), parts.getOrNull(1) ?: "dark")
                }
            }
        )
    }

    private fun applyPageTheme(color: String, mode: String) {
        pageBackground = runCatching { color.toColorInt() }.getOrDefault(DEFAULT_BACKGROUND)
        val light = mode == "light"
        root.setBackgroundColor(pageBackground)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    /**
     * A WebMessageListener is the supported replacement for
     * addJavascriptInterface: it is bound to our own origin only and hands the
     * page no reflection surface.
     */
    private fun injectThemeReporter(view: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        view.evaluateJavascript(THEME_REPORTER, null)
    }

    private fun wireBackNavigation() {
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else isEnabled = false
            }
        }
        // Disabled while there is no in-page history, so the system keeps Back
        // and the predictive-back preview of the home screen stays correct.
        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    private fun openExternally(url: Uri) {
        val scheme = url.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") return
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this, url)
        } catch (_: ActivityNotFoundException) {
            // No browser installed at all: better to do nothing than to crash.
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        root.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val ASSET_HOST = "appassets.androidplatform.net"
        const val START_URL = "https://$ASSET_HOST/assets/www/index.html"
        const val HOST_OBJECT = "solarCastShell"
        val DEFAULT_BACKGROUND = "#100E17".toColorInt()

        /**
         * Reports the page's resolved background and light/dark mode once, and
         * again whenever the page rewrites the attributes it themes itself with.
         */
        val THEME_REPORTER = """
            (function () {
              if (window.__solarCastThemeBridge) return;
              window.__solarCastThemeBridge = true;
              var root = document.documentElement;
              function rgbToHex(v) {
                var m = /rgba?\((\d+)[,\s]+(\d+)[,\s]+(\d+)/.exec(v || '');
                if (!m) return '';
                return '#' + [1, 2, 3].map(function (i) {
                  return ('0' + (+m[i]).toString(16)).slice(-2);
                }).join('');
              }
              function report() {
                var bg = rgbToHex(getComputedStyle(document.body).backgroundColor);
                var mode = root.getAttribute('data-mode') || 'dark';
                if (bg) $HOST_OBJECT.postMessage(bg + '|' + mode);
              }
              new MutationObserver(report).observe(root, {
                attributes: true,
                attributeFilter: ['data-mode', 'data-scheme', 'style', 'class']
              });
              report();
            })();
        """.trimIndent()
    }
}
