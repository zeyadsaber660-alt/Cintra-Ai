package com.cintra.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Clean, modern 1.8dp stroke icon set (Lucide / SF Symbols / ChatGPT aesthetic).
 * Replaces bulky Material Filled icons across Cintra.
 */
object CintraIcons {

    private fun strokeIcon(
        name: String,
        strokeWidth: Float = 1.85f,
        block: PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    ).build()

    val MenuTwoLines: ImageVector by lazy {
        strokeIcon("MenuTwoLines", 2f) {
            moveTo(4f, 8.5f); lineTo(20f, 8.5f)
            moveTo(4f, 15.5f); lineTo(15f, 15.5f)
        }
    }

    val NewChatSquare: ImageVector by lazy {
        strokeIcon("NewChatSquare") {
            moveTo(12f, 3f); lineTo(5f, 3f); curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
            lineTo(3f, 19f); curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
            lineTo(19f, 21f); curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f); lineTo(21f, 12f)
            moveTo(18.4f, 2.6f); curveTo(19.2f, 1.8f, 20.5f, 1.8f, 21.3f, 2.6f)
            curveTo(22.1f, 3.4f, 22.1f, 4.7f, 21.3f, 5.5f)
            lineTo(12.2f, 14.6f); lineTo(8.5f, 15.5f); lineTo(9.4f, 11.8f); close()
        }
    }

    val Search: ImageVector by lazy {
        strokeIcon("Search") {
            moveTo(11f, 19f); curveTo(15.42f, 19f, 19f, 15.42f, 19f, 11f)
            curveTo(19f, 6.58f, 15.42f, 3f, 11f, 3f)
            curveTo(6.58f, 3f, 3f, 6.58f, 3f, 11f)
            curveTo(3f, 15.42f, 6.58f, 19f, 11f, 19f)
            moveTo(21f, 21f); lineTo(16.65f, 16.65f)
        }
    }

    val Sun: ImageVector by lazy {
        strokeIcon("Sun") {
            moveTo(12f, 16f); curveTo(14.21f, 16f, 16f, 14.21f, 16f, 12f)
            curveTo(16f, 9.79f, 14.21f, 8f, 12f, 8f)
            curveTo(9.79f, 8f, 8f, 9.79f, 8f, 12f)
            curveTo(8f, 14.21f, 9.79f, 16f, 12f, 16f)
            moveTo(12f, 2f); lineTo(12f, 4f)
            moveTo(12f, 20f); lineTo(12f, 22f)
            moveTo(4.93f, 4.93f); lineTo(6.34f, 6.34f)
            moveTo(17.66f, 17.66f); lineTo(19.07f, 19.07f)
            moveTo(2f, 12f); lineTo(4f, 12f)
            moveTo(20f, 12f); lineTo(22f, 12f)
            moveTo(6.34f, 17.66f); lineTo(4.93f, 19.07f)
            moveTo(19.07f, 4.93f); lineTo(17.66f, 6.34f)
        }
    }

    val Moon: ImageVector by lazy {
        strokeIcon("Moon") {
            moveTo(20.5f, 14.5f)
            curveTo(19.3f, 15.1f, 17.9f, 15.4f, 16.5f, 15.2f)
            curveTo(12.2f, 14.6f, 9.1f, 10.7f, 9.5f, 6.3f)
            curveTo(9.6f, 5.1f, 10.0f, 3.9f, 10.6f, 3.0f)
            curveTo(6.3f, 3.6f, 3.0f, 7.3f, 3.0f, 11.8f)
            curveTo(3.0f, 16.9f, 7.1f, 21.0f, 12.2f, 21.0f)
            curveTo(16.1f, 21.0f, 19.4f, 18.5f, 20.5f, 14.5f)
            close()
        }
    }

    val ChevronDown: ImageVector by lazy {
        strokeIcon("ChevronDown") {
            moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
        }
    }

    val ChevronEnd: ImageVector by lazy {
        strokeIcon("ChevronEnd") {
            moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f)
        }
    }

    val ArrowBack: ImageVector by lazy {
        strokeIcon("ArrowBack") {
            moveTo(19f, 12f); lineTo(5f, 12f)
            moveTo(12f, 19f); lineTo(5f, 12f); lineTo(12f, 5f)
        }
    }

    val Plus: ImageVector by lazy {
        strokeIcon("Plus", 2f) {
            moveTo(12f, 5f); lineTo(12f, 19f)
            moveTo(5f, 12f); lineTo(19f, 12f)
        }
    }

    val Close: ImageVector by lazy {
        strokeIcon("Close", 2f) {
            moveTo(18f, 6f); lineTo(6f, 18f)
            moveTo(6f, 6f); lineTo(18f, 18f)
        }
    }

    val ArrowUp: ImageVector by lazy {
        strokeIcon("ArrowUp", 2.1f) {
            moveTo(12f, 19f); lineTo(12f, 5f)
            moveTo(6f, 11f); lineTo(12f, 5f); lineTo(18f, 11f)
        }
    }

    val Mic: ImageVector by lazy {
        strokeIcon("Mic") {
            moveTo(12f, 2f); curveTo(10.34f, 2f, 9f, 3.34f, 9f, 5f)
            lineTo(9f, 11f); curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
            curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f); lineTo(15f, 5f)
            curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
            moveTo(19f, 10f); lineTo(19f, 11f)
            curveTo(19f, 14.87f, 15.87f, 18f, 12f, 18f)
            curveTo(8.13f, 18f, 5f, 14.87f, 5f, 11f); lineTo(5f, 10f)
            moveTo(12f, 18f); lineTo(12f, 22f)
            moveTo(8.5f, 22f); lineTo(15.5f, 22f)
        }
    }

    val Waveform: ImageVector by lazy {
        strokeIcon("Waveform", 2.1f) {
            moveTo(4f, 10f); lineTo(4f, 14f)
            moveTo(8f, 6f); lineTo(8f, 18f)
            moveTo(12f, 3f); lineTo(12f, 21f)
            moveTo(16f, 7f); lineTo(16f, 17f)
            moveTo(20f, 10f); lineTo(20f, 14f)
        }
    }

    val Images: ImageVector by lazy {
        strokeIcon("Images") {
            moveTo(5f, 5f); lineTo(17f, 5f); curveTo(18.1f, 5f, 19f, 5.9f, 19f, 7f)
            lineTo(19f, 17f); curveTo(19f, 18.1f, 18.1f, 19f, 17f, 19f)
            lineTo(5f, 19f); curveTo(3.9f, 19f, 3f, 18.1f, 3f, 17f)
            lineTo(3f, 7f); curveTo(3f, 5.9f, 3.9f, 5f, 5f, 5f)
            moveTo(3f, 15f); lineTo(8f, 10.5f); lineTo(13f, 15f); lineTo(16f, 12.5f); lineTo(19f, 15f)
            moveTo(14.5f, 9f); lineTo(14.51f, 9f)
        }
    }

    val Library: ImageVector by lazy {
        strokeIcon("Library") {
            moveTo(4f, 5f); lineTo(4f, 19f); lineTo(8f, 19f); lineTo(8f, 5f); close()
            moveTo(10f, 5f); lineTo(10f, 19f); lineTo(14f, 19f); lineTo(14f, 5f); close()
            moveTo(16f, 6f); lineTo(19.5f, 19f); lineTo(21.5f, 18.4f); lineTo(18f, 5.4f); close()
        }
    }

    val Sliders: ImageVector by lazy {
        strokeIcon("Sliders") {
            moveTo(4f, 8f); lineTo(14f, 8f); moveTo(18f, 8f); lineTo(20f, 8f)
            moveTo(16f, 6f); curveTo(17.1f, 6f, 18f, 6.9f, 18f, 8f)
            curveTo(18f, 9.1f, 17.1f, 10f, 16f, 10f); curveTo(14.9f, 10f, 14f, 9.1f, 14f, 8f)
            curveTo(14f, 6.9f, 14.9f, 6f, 16f, 6f)
            moveTo(4f, 16f); lineTo(8f, 16f); moveTo(12f, 16f); lineTo(20f, 16f)
            moveTo(10f, 14f); curveTo(11.1f, 14f, 12f, 14.9f, 12f, 16f)
            curveTo(12f, 17.1f, 11.1f, 18f, 10f, 18f); curveTo(8.9f, 18f, 8f, 17.1f, 8f, 16f)
            curveTo(8f, 14.9f, 8.9f, 14f, 10f, 14f)
        }
    }

    val Folder: ImageVector by lazy {
        strokeIcon("Folder") {
            moveTo(3f, 7f); curveTo(3f, 5.9f, 3.9f, 5f, 5f, 5f)
            lineTo(9.5f, 5f); lineTo(11.5f, 7.5f); lineTo(19f, 7.5f)
            curveTo(20.1f, 7.5f, 21f, 8.4f, 21f, 9.5f); lineTo(21f, 17f)
            curveTo(21f, 18.1f, 20.1f, 19f, 19f, 19f); lineTo(5f, 19f)
            curveTo(3.9f, 19f, 3f, 18.1f, 3f, 17f); close()
        }
    }

    val ExploreGrid: ImageVector by lazy {
        strokeIcon("ExploreGrid") {
            moveTo(4f, 4f); lineTo(10f, 4f); lineTo(10f, 10f); lineTo(4f, 10f); close()
            moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f); lineTo(14f, 10f); close()
            moveTo(4f, 14f); lineTo(10f, 14f); lineTo(10f, 20f); lineTo(4f, 20f); close()
            moveTo(17f, 13.5f); lineTo(20.5f, 17f); lineTo(17f, 20.5f); lineTo(13.5f, 17f); close()
        }
    }

    val Sparkle: ImageVector by lazy {
        strokeIcon("Sparkle") {
            moveTo(12f, 3f); lineTo(14.2f, 9.8f); lineTo(21f, 12f)
            lineTo(14.2f, 14.2f); lineTo(12f, 21f); lineTo(9.8f, 14.2f)
            lineTo(3f, 12f); lineTo(9.8f, 9.8f); close()
        }
    }

    val SettingsGear: ImageVector by lazy {
        strokeIcon("SettingsGear") {
            moveTo(12f, 15f); curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            moveTo(19.4f, 15f); lineTo(21f, 13.4f); lineTo(21f, 10.6f); lineTo(19.4f, 9f)
            lineTo(19.8f, 6.8f); lineTo(17.8f, 4.8f); lineTo(15.6f, 5.2f)
            lineTo(14f, 3.6f); lineTo(10f, 3.6f); lineTo(8.4f, 5.2f)
            lineTo(6.2f, 4.8f); lineTo(4.2f, 6.8f); lineTo(4.6f, 9f)
            lineTo(3f, 10.6f); lineTo(3f, 13.4f); lineTo(4.6f, 15f)
            lineTo(4.2f, 17.2f); lineTo(6.2f, 19.2f); lineTo(8.4f, 18.8f)
            lineTo(10f, 20.4f); lineTo(14f, 20.4f); lineTo(15.6f, 18.8f)
            lineTo(17.8f, 19.2f); lineTo(19.8f, 17.2f); close()
        }
    }

    val Bell: ImageVector by lazy {
        strokeIcon("Bell") {
            moveTo(18f, 8f); curveTo(18f, 4.69f, 15.31f, 2f, 12f, 2f)
            curveTo(8.69f, 2f, 6f, 4.69f, 6f, 8f); lineTo(6f, 14f)
            lineTo(4f, 17f); lineTo(20f, 17f); lineTo(18f, 14f); close()
            moveTo(10f, 20f); curveTo(10.5f, 21f, 13.5f, 21f, 14f, 20f)
        }
    }

    val ShieldCheck: ImageVector by lazy {
        strokeIcon("ShieldCheck") {
            moveTo(12f, 3f); lineTo(19f, 6f); lineTo(19f, 12f)
            curveTo(19f, 16.5f, 15.8f, 19.8f, 12f, 21f)
            curveTo(8.2f, 19.8f, 5f, 16.5f, 5f, 12f); lineTo(5f, 6f); close()
            moveTo(9f, 12f); lineTo(11f, 14f); lineTo(15.5f, 9.5f)
        }
    }

    val Storage: ImageVector by lazy {
        strokeIcon("Storage") {
            moveTo(4f, 6f); lineTo(20f, 6f); lineTo(20f, 18f); lineTo(4f, 18f); close()
            moveTo(4f, 12f); lineTo(20f, 12f)
            moveTo(8f, 9f); lineTo(8.01f, 9f)
            moveTo(8f, 15f); lineTo(8.01f, 15f)
        }
    }

    val Globe: ImageVector by lazy {
        strokeIcon("Globe") {
            moveTo(12f, 21f); curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            moveTo(3f, 12f); lineTo(21f, 12f)
            moveTo(12f, 3f); curveTo(14.5f, 6f, 14.5f, 18f, 12f, 21f)
            moveTo(12f, 3f); curveTo(9.5f, 6f, 9.5f, 18f, 12f, 21f)
        }
    }

    val Info: ImageVector by lazy {
        strokeIcon("Info") {
            moveTo(12f, 21f); curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            moveTo(12f, 16f); lineTo(12f, 11.5f)
            moveTo(12f, 8.2f); lineTo(12.01f, 8.2f)
        }
    }

    val LogOut: ImageVector by lazy {
        strokeIcon("LogOut") {
            moveTo(9f, 21f); lineTo(5f, 21f); curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f)
            lineTo(3f, 5f); curveTo(3f, 3.9f, 3.9f, 3f, 5f, 3f); lineTo(9f, 3f)
            moveTo(16f, 17f); lineTo(21f, 12f); lineTo(16f, 7f)
            moveTo(21f, 12f); lineTo(9f, 12f)
        }
    }

    val SwitchAccount: ImageVector by lazy {
        strokeIcon("SwitchAccount") {
            moveTo(17f, 2f); lineTo(21f, 6f); lineTo(17f, 10f)
            moveTo(3f, 11f); lineTo(3f, 9f); curveTo(3f, 7.34f, 4.34f, 6f, 6f, 6f); lineTo(21f, 6f)
            moveTo(7f, 22f); lineTo(3f, 18f); lineTo(7f, 14f)
            moveTo(21f, 13f); lineTo(21f, 15f); curveTo(21f, 16.66f, 19.66f, 18f, 18f, 18f); lineTo(3f, 18f)
        }
    }

    val Brain: ImageVector by lazy {
        strokeIcon("Brain") {
            moveTo(9.5f, 4f); curveTo(6.5f, 4f, 4.5f, 6.2f, 4.5f, 9f)
            curveTo(4.5f, 10.5f, 5.1f, 11.7f, 6f, 12.5f)
            curveTo(5.2f, 13.4f, 4.8f, 14.5f, 5f, 15.8f)
            curveTo(5.3f, 18.2f, 7.3f, 20f, 9.8f, 20f); lineTo(11.5f, 20f); lineTo(11.5f, 4f); close()
            moveTo(14.5f, 4f); curveTo(17.5f, 4f, 19.5f, 6.2f, 19.5f, 9f)
            curveTo(19.5f, 10.5f, 18.9f, 11.7f, 18f, 12.5f)
            curveTo(18.8f, 13.4f, 19.2f, 14.5f, 19f, 15.8f)
            curveTo(18.7f, 18.2f, 16.7f, 20f, 14.2f, 20f); lineTo(12.5f, 20f); lineTo(12.5f, 4f); close()
        }
    }

    val HistoryBox: ImageVector by lazy {
        strokeIcon("HistoryBox") {
            moveTo(3f, 7f); lineTo(21f, 7f); lineTo(21f, 10f); lineTo(3f, 10f); close()
            moveTo(5f, 10f); lineTo(5f, 19f); lineTo(19f, 19f); lineTo(19f, 10f)
            moveTo(10f, 14f); lineTo(14f, 14f)
        }
    }

    val UsageChart: ImageVector by lazy {
        strokeIcon("UsageChart") {
            moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
            moveTo(7f, 14f); lineTo(10.5f, 10.5f); lineTo(13.5f, 13.5f); lineTo(17f, 9.5f)
        }
    }

    val MoreDots: ImageVector by lazy {
        strokeIcon("MoreDots", 2.4f) {
            moveTo(5.5f, 12f); lineTo(5.51f, 12f)
            moveTo(12f, 12f); lineTo(12.01f, 12f)
            moveTo(18.5f, 12f); lineTo(18.51f, 12f)
        }
    }

    val Check: ImageVector by lazy {
        strokeIcon("Check", 2.1f) {
            moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7.5f)
        }
    }

    val StopSquare: ImageVector by lazy {
        strokeIcon("StopSquare", 2f) {
            moveTo(7f, 7f); lineTo(17f, 7f); lineTo(17f, 17f); lineTo(7f, 17f); close()
        }
    }
}
