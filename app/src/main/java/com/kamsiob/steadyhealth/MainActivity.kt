package com.kamsiob.steadyhealth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.kamsiob.steadyhealth.data.ContextRepository
import com.kamsiob.steadyhealth.data.DailyPromptRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.remind.Reminding
import com.kamsiob.steadyhealth.sensing.DaySteps
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
        readTheStepCounter()
        val fromWidget = intent?.getBooleanExtra(START_SESSION, false) == true
        intent?.removeExtra(START_SESSION)
        setContent {
            SteadyTheme {
                SteadyApp(openSession = fromWidget)
            }
        }
    }

    /**
     * The phone's own step count, read once on the way in. Part 8 item 1.
     *
     * Here rather than in a service or a periodic job, because the app has neither
     * and is not getting one. The counter is asked once when somebody opens the app,
     * the listener unregisters itself on the first reading, and what is written down
     * is that reading and today's date. Nothing wakes the phone for it.
     *
     * It does nothing at all until somebody turns the line on, since [DaySteps]
     * checks the permission first and returns without touching the sensor.
     */
    private fun readTheStepCounter() {
        val steps = DaySteps(applicationContext)
        if (!steps.available || !steps.allowed) return
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        steps.readOnce { sinceBoot ->
            lifecycleScope.launch {
                ContextRepository(SteadyDatabase.get(applicationContext))
                    .setStepReading(today, sinceBoot)
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
