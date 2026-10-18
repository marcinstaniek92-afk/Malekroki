package com.malekroki.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malekroki.app.data.AppSettings
import com.malekroki.app.domain.ThemeMode
import com.malekroki.app.ui.navigation.AppNavigation
import com.malekroki.app.ui.theme.MaleKrokiTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as MaleKrokiApp).container

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())

            // Keep status/navigation bar icons readable when the in-app theme differs from the system one.
            LaunchedEffect(settings.themeMode) {
                val style = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> null
                    ThemeMode.LIGHT -> SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    ThemeMode.DARK -> SystemBarStyle.dark(Color.TRANSPARENT)
                }
                if (style == null) {
                    enableEdgeToEdge()
                } else {
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
            }

            MaleKrokiTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                AppNavigation()
            }
        }
    }
}
