package com.kamsiob.steadyhealth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kamsiob.steadyhealth.ui.SteadyApp
import com.kamsiob.steadyhealth.ui.theme.SteadyTheme

/**
 * The only activity.
 *
 * Single activity because navigation is three tabs and a stack, and because every
 * screen shares one theme and one back behaviour. Edge to edge so the hero can
 * run under the status bar, with each screen taking its own insets.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SteadyTheme {
                SteadyApp()
            }
        }
    }
}
