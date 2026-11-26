package com.cintra.app

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cintra.app.ui.chat.CintraChatScreen
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Single Compose host. Edge-to-edge is enabled so the floating glass chrome can
 * sit behind the status/navigation bars and blur live content underneath.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        runCatching { WebView.enableSlowWholeDocumentDraw() }
        runCatching { PDFBoxResourceLoader.init(applicationContext) }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CintraChatScreen()
        }
    }
}