package com.kamsiob.steadyhealth.sensing

import kotlin.math.abs
import kotlin.math.sqrt

/** One accelerometer sample: three axes in m/s^2, and when it was taken. */
data class Sample(val x: Float, val y: Float, val z: Float, val atNanos: Long) {
    val magnitude: Double get() = sqrt((x * x + y * y + z * z).toDouble())
}

/**
 * Counting repetitions from the phone's motion sensor.
 *
 * Grid screen 10: the phone sits in a pocket and times each stand from the motion
 * sensor, with no camera. A chair stand is one push up against gravity followed
 * by one controlled descent, and in the acceleration magnitude that is a clear
 * peak above one g followed by a dip below it.
 *
 * Written as a value-in, value-out counter with no Android types so that the
 * whole of it can be tested against recorded and synthetic traces without a
 * device. The listener that feeds it is the only part that needs a phone.
 *
 * It is deliberately conservative. Under-counting means somebody's number is a
 * little low and they can count by hand instead; over-counting means the app
 * tells somebody they did fourteen stands when they did nine, which is a lie
 * about their body.
 */
class RepCounter(
    /** How far above resting the magnitude must go, in m/s^2. */
    private val rise: Double = DEFAULT_RISE,
    /** The shortest believable gap between two repetitions, in milliseconds. */
    private val refractoryMillis: Long = DEFAULT_REFRACTORY,
    /** How strongly to smooth. Higher is smoother and slower to react. */
    private val smoothing: Double = DEFAULT_SMOOTHING,
) {

    private var resting = GRAVITY
    private var smoothed = GRAVITY
    private var above = false
    private var lastRepNanos = 0L
    private var seen = 0

    var count: Int = 0
        private set

    /** Feed one sample. Returns true when this sample completed a repetition. */
    fun accept(sample: Sample): Boolean {
        val magnitude = sample.magnitude
        seen += 1

        // The first samples establish what still looks like for this phone in
        // this pocket, rather than assuming exactly one g.
        if (seen <= SETTLING_SAMPLES) {
            resting = resting * (1 - SETTLE_RATE) + magnitude * SETTLE_RATE
            smoothed = magnitude
            return false
        }

        smoothed = smoothed * smoothing + magnitude * (1 - smoothing)
        val deviation = smoothed - resting

        if (!above && deviation > rise) {
            above = true
            return false
        }

        if (above && deviation < rise * RELEASE) {
            above = false
            val sinceLast = (sample.atNanos - lastRepNanos) / NANOS_PER_MILLI
            if (lastRepNanos == 0L || sinceLast >= refractoryMillis) {
                lastRepNanos = sample.atNanos
                count += 1
                return true
            }
        }

        // A phone that has been still for a while re-learns its resting value, so
        // somebody who shifts in the chair between stands does not lose the trace.
        if (!above && abs(deviation) < DRIFT) {
            resting = resting * (1 - DRIFT_RATE) + smoothed * DRIFT_RATE
        }
        return false
    }

    fun reset() {
        resting = GRAVITY
        smoothed = GRAVITY
        above = false
        lastRepNanos = 0
        seen = 0
        count = 0
    }

    companion object {
        const val GRAVITY = 9.81

        /**
         * A chair stand pushes the phone up hard enough to clear this. Set from
         * traces on the device rather than from theory, and set high rather than
         * low for the reason in the class comment.
         */
        const val DEFAULT_RISE = 2.2

        /** Nobody stands up from a chair twice in under three quarters of a second. */
        const val DEFAULT_REFRACTORY = 750L

        const val DEFAULT_SMOOTHING = 0.72

        /** A stand: a whole body pushed up out of a chair. */
        fun forStands() = RepCounter()

        /** A step: lighter and faster than a stand, so a lower bar and a shorter gap. */
        fun forSteps() = RepCounter(rise = 1.4, refractoryMillis = 300)

        private const val SETTLING_SAMPLES = 20
        private const val SETTLE_RATE = 0.2
        private const val RELEASE = 0.4
        private const val DRIFT = 0.35
        private const val DRIFT_RATE = 0.01
        private const val NANOS_PER_MILLI = 1_000_000.0
    }
}
