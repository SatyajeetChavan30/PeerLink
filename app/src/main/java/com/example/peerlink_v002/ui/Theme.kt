package com.example.peerlink_v002.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PeerLinkBlack = Color(0xFF000000)
private val PeerLinkWhite = Color(0xFFFFFFFF)
private val PeerLinkDarkGray = Color(0xFF121212)

private val DarkColorScheme = darkColorScheme(
    primary = PeerLinkWhite,
    onPrimary = PeerLinkBlack,
    secondary = PeerLinkWhite,
    onSecondary = PeerLinkBlack,
    tertiary = PeerLinkWhite,
    onTertiary = PeerLinkBlack,
    background = PeerLinkBlack,
    onBackground = PeerLinkWhite,
    surface = PeerLinkBlack,
    onSurface = PeerLinkWhite,
    surfaceVariant = PeerLinkDarkGray,
    onSurfaceVariant = PeerLinkWhite,
    error = PeerLinkWhite,
    onError = PeerLinkBlack
)

// We enforce Dark Mode (Black & White)
@Composable
fun PeerLinkTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // White text
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
