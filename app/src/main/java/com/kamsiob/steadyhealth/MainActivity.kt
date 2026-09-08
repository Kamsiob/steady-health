package com.kamsiob.steadyhealth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.kamsiob.steadyhealth.data.DailyPromptRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.remind.Reminding
import com.kamsiob.steadyhealth.ui.SteadyApp
import com.kamsiob.steadyhealth.ui.theme.SteadyTheme
import com.kamsiob.steadyhealth.widget.START_SESSION
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

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
        recordOpenedFromPrompt()
        val fromWidget = intent?.getBooleanExtra(START_SESSION, false) == true
        intent?.removeExtra(START_SESSION)
        setContent {
            SteadyTheme {
                SteadyApp(openSession = fromWidget)
            }
        }
    }

    /**
     * The daily prompt was tapped rather than ignored.
     *
     * The only thing this app ever records about a notification, and it records it so
     * the prompt knows when to stop asking. Nothing counts it, nothing shows it, and
     * nothing anywhere is a streak of them.
     */
    private fun recordOpenedFromPrompt() {
        if (intent?.getBooleanExtra(Reminding.FROM_DAILY, false) != true) return
        intent.removeExtra(Reminding.FROM_DAILY)
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        lifecycleScope.launch {
            DailyPromptRepository(SteadyDatabase.get(applicationContext)).opened(today)
        }
    }
}
