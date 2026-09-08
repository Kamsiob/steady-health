package com.kamsiob.steadyhealth.session

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The session's haptics.
 *
 * ADDENDUM-03 Part 17: a buzz on every rep, every set and every rest end, so a
 * session works without sight or sound. That is not a flourish. Somebody with the
 * phone in a pocket, doing chair stands, with the sound off, has the haptics and
 * nothing else, and the whole session has to be followable through them.
 *
 * Three distinct patterns, so they are distinguishable through a trouser pocket: a
 * tick for a rep, two for the end of a set, three for the end of a rest.
 */
class Buzz(context: Context) {

    /**
     * The vibrator, however this Android version hands it over.
     *
     * `VibratorManager` arrived in API 31 and the minimum here is 29, so both paths
     * exist. The app works the same on a device with no vibrator at all.
     */
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }?.takeIf { it.hasVibrator() }

    var on: Boolean = true

    /** One rep counted. The lightest of the three, because it happens most. */
    fun rep() = play(longArrayOf(0, REP_MILLIS), REP_AMPLITUDE)

    /** The end of a set. Two, so it is not mistaken for a rep. */
    fun setDone() = play(longArrayOf(0, SHORT, GAP, LONG), FULL)

    /** The end of a rest. Three, rising, because it means move. */
    fun restOver() = play(longArrayOf(0, SHORT, GAP, SHORT, GAP, LONG), FULL)

    private fun play(pattern: LongArray, amplitude: Int) {
        if (!on) return
        val device = vibrator ?: return
        val amplitudes = IntArray(pattern.size) { index ->
            if (index % 2 == 0) 0 else amplitude
        }
        runCatching {
            device.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        }
    }

    private companion object {
        const val REP_MILLIS = 25L
        const val SHORT = 30L
        const val LONG = 70L
        const val GAP = 60L
        const val REP_AMPLITUDE = 120
        const val FULL = 200
    }
}
