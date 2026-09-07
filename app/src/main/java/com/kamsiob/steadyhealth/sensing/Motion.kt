package com.kamsiob.steadyhealth.sensing

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The phone's accelerometer, as a flow of counts.
 *
 * The only part of counting that needs a device. Everything that decides what a
 * repetition is lives in [RepCounter], which has no Android types in it and is
 * tested without one.
 *
 * The sensor is registered when somebody collects and unregistered the moment
 * they stop, with no listener left behind: a check that quietly kept the
 * accelerometer awake would be a battery leak in an app used by people who charge
 * their phone once a day.
 */
class Motion(private val context: Context) {

    val available: Boolean
        get() = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    private val manager: SensorManager?
        get() = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    /**
     * Counts, emitted as they happen, starting at zero.
     *
     * [counter] decides what a repetition is; this only feeds it. The first value
     * is emitted immediately so a screen has something to draw before anybody
     * moves.
     */
    fun counts(counter: RepCounter): Flow<Int> = callbackFlow {
        val sensors = manager
        val accelerometer = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensors == null || accelerometer == null) {
            trySend(0)
            awaitClose { }
            return@callbackFlow
        }

        counter.reset()
        trySend(0)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val sample = Sample(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    atNanos = event.timestamp,
                )
                if (counter.accept(sample)) trySend(counter.count)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensors.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensors.unregisterListener(listener) }
    }
}
