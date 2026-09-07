package com.kamsiob.steadyhealth.sensing

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.sin

/**
 * Counting from the motion sensor, without a phone.
 *
 * The traces are synthetic, which is a real limit and is stated here rather than
 * hidden: they show the counter does what it is written to do, not that a Pixel
 * in somebody's pocket produces this shape. The device test does that. What these
 * catch is the whole class of mistakes that make a counter useless: counting
 * stillness, counting a bump, counting one movement twice.
 */
class RepCounterTest {

    @Test
    fun stillnessIsNotARepetition() {
        val counter = RepCounter.forStands()
        still(seconds = 30).forEach(counter::accept)
        assertThat(counter.count).isEqualTo(0)
    }

    @Test
    fun aHandfulOfStandsCountsAsThatMany() {
        val counter = RepCounter.forStands()
        trace(reps = 10, periodMillis = 2000, peak = 4.0).forEach(counter::accept)
        assertThat(counter.count).isEqualTo(10)
    }

    @Test
    fun countingIsNeverHigherThanTheTruth() {
        // The one failure that matters. Under-counting means somebody's number is
        // a little low and they can count by hand. Over-counting is the app
        // telling somebody they did fourteen when they did nine.
        listOf(5, 10, 14, 20).forEach { reps ->
            listOf(1500L, 2000L, 2500L).forEach { period ->
                val counter = RepCounter.forStands()
                trace(reps, period, peak = 4.0).forEach(counter::accept)
                assertWithMessage("$reps stands at ${period}ms")
                    .that(counter.count)
                    .isAtMost(reps)
            }
        }
    }

    @Test
    fun oneJoltIsNotTwoRepetitions() {
        val counter = RepCounter.forStands()
        still(seconds = 2).forEach(counter::accept)
        jolt(atSecond = 2, peak = 6.0).forEach(counter::accept)
        still(seconds = 2, from = 3).forEach(counter::accept)
        assertThat(counter.count).isAtMost(1)
    }

    @Test
    fun aPhoneShiftingInAPocketIsNotARepetition() {
        val counter = RepCounter.forStands()
        val drifting = (0 until SAMPLES_PER_SECOND * 30).map { i ->
            val t = i / SAMPLES_PER_SECOND.toDouble()
            sample(RepCounter.GRAVITY + 0.6 * sin(t / 5), i)
        }
        drifting.forEach(counter::accept)
        assertThat(counter.count).isEqualTo(0)
    }

    @Test
    fun steppingCountsFasterMovementThanStandingDoes() {
        val stands = RepCounter.forStands()
        val steps = RepCounter.forSteps()
        val fast = trace(reps = 40, periodMillis = 600, peak = 2.6)
        fast.forEach(stands::accept)
        fast.forEach(steps::accept)
        assertThat(steps.count).isGreaterThan(stands.count)
    }

    @Test
    fun resetForgetsEverything() {
        val counter = RepCounter.forStands()
        trace(reps = 6, periodMillis = 2000, peak = 4.0).forEach(counter::accept)
        assertThat(counter.count).isGreaterThan(0)
        counter.reset()
        assertThat(counter.count).isEqualTo(0)
    }

    /** A phone lying still: gravity, plus the noise every sensor has. */
    private fun still(seconds: Int, from: Int = 0): List<Sample> =
        (0 until SAMPLES_PER_SECOND * seconds).map { i ->
            val index = from * SAMPLES_PER_SECOND + i
            sample(RepCounter.GRAVITY + NOISE * sin(index * 0.7), index)
        }

    /**
     * [reps] repetitions, one every [periodMillis], each a half-sine of height
     * [peak] above gravity followed by a return to rest.
     */
    private fun trace(reps: Int, periodMillis: Long, peak: Double): List<Sample> {
        val settle = still(seconds = 2)
        val perRep = (periodMillis * SAMPLES_PER_SECOND / MILLIS_PER_SECOND).toInt()
        val body = (0 until reps * perRep).map { i ->
            val phase = (i % perRep) / perRep.toDouble()
            // The push is the first third of each repetition; the rest is sitting
            // back down and waiting, which is where the trace returns to rest.
            val lift = if (phase < PUSH) sin(phase / PUSH * Math.PI) * peak else 0.0
            sample(RepCounter.GRAVITY + lift + NOISE * sin(i * 0.7), settle.size + i)
        }
        return settle + body
    }

    private fun jolt(atSecond: Int, peak: Double): List<Sample> =
        (0 until SAMPLES_PER_SECOND / 2).map { i ->
            val phase = i / (SAMPLES_PER_SECOND / 2.0)
            sample(
                RepCounter.GRAVITY + sin(phase * Math.PI) * peak,
                atSecond * SAMPLES_PER_SECOND + i,
            )
        }

    /** One sample of the given magnitude, on the axis gravity is on. */
    private fun sample(magnitude: Double, index: Int) = Sample(
        x = 0f,
        y = magnitude.toFloat(),
        z = 0f,
        atNanos = index.toLong() * NANOS_PER_SAMPLE,
    )

    private companion object {
        const val SAMPLES_PER_SECOND = 50
        const val MILLIS_PER_SECOND = 1000
        const val NANOS_PER_SAMPLE = 20_000_000L
        const val NOISE = 0.05
        const val PUSH = 0.35
    }
}
