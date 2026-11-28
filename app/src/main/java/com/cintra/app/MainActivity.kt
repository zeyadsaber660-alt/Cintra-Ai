package com.cintra.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cintra.app.ui.chat.CintraChatScreen
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Tracks whether Cintra is currently visible on screen so completion
 * notifications fire cleanly whenever the user is outside the app.
 */
object CintraAppState {
    @Volatile
    var isInForeground: Boolean = true
}

/**
 * Single Compose host. Edge-to-edge is enabled so the floating glass chrome can
 * sit behind the status/navigation bars and blur live content underneath.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        runCatching { WebView.enableSlowWholeDocumentDraw() }
        runCatching { PDFBoxResourceLoader.init(applicationContext) }
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            CintraChatScreen()
        }
    }

    override fun onStart() {
        super.onStart()
        CintraAppState.isInForeground = true
    }

    override fun onStop() {
        super.onStop()
        CintraAppState.isInForeground = false
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                runCatching {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1001,
                    )
                }
            }
        }
    }
}