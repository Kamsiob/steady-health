package com.kamsiob.steadyhealth.sensing

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat

/**
 * The phone's own step counter, read once and let go of.
 *
 * ADDENDUM-03 Part 8 item 1 wants "steps and time spent up, from the phone", which
 * means the hardware counter rather than the accelerometer work a session does. The
 * two are different things: a session counts what somebody is doing right now while
 * the app is open, and this is a number the phone has been keeping on its own with
 * nothing running.
 *
 * TYPE_STEP_COUNTER returns steps since the phone last booted, not steps today, and
 * it resets to zero on a reboot. So a reading on its own says nothing; what says
 * something is the difference between two readings, which is why what is stored is a
 * reading with the day it was taken and the arithmetic happens later.
 *
 * NOTHING RUNS IN THE BACKGROUND. There is no service, no periodic wake, and no
 * listener that outlives the screen. The counter is read when the app is opened and
 * the listener is unregistered in the same breath. A step count that needed a service
 * would be an app that runs when nobody asked it to, which is a different app.
 *
 * ACTIVITY_RECOGNITION is a runtime permission from API 29. Without it this returns
 * null and the line is never shown, which is the whole of the failure handling: Part 8
 * calls this optional and the app is complete without it.
 */
class DaySteps(private val context: Context) {

    private val manager = ContextCompat.getSystemService(context, SensorManager::class.java)

    /** Whether this phone has the counter at all. Plenty do not. */
    val available: Boolean
        get() = context.packageManager.hasSystemFeature(COUNTER) &&
            manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    val allowed: Boolean
        get() = ContextCompat.checkSelfPermission(context, ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * One reading, then stop listening.
     *
     * [onSteps] gets the count since boot. It may never be called, on a phone with no
     * counter, with the permission refused, or where the sensor simply does not report
     * before the caller gives up, and every one of those is an ordinary outcome rather
     * than an error worth a screen.
     */
    fun readOnce(onSteps: (Long) -> Unit) {
        if (!available || !allowed) return
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                manager.unregisterListener(this)
                onSteps(event.values.firstOrNull()?.toLong() ?: return)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    companion object {
        /**
         * The literal rather than the constant, which is API 29 and would be inlined
         * into a build whose minimum is 29. The manifest declares the same string.
         */
        const val ACTIVITY_RECOGNITION = "android.permission.ACTIVITY_RECOGNITION"

        private const val COUNTER = PackageManager.FEATURE_SENSOR_STEP_COUNTER
    }
}
