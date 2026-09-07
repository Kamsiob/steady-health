package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/** LOGIC.md section 1, as tests. */
class WeightEngineTest {

    @Test
    fun theFirstReadingSeedsTheAverageWithItself() {
        val series = WeightEngine.smooth(listOf(Reading(1, 80.0)))

        assertThat(series.single().smoothedKg).isEqualTo(80.0)
    }

    @Test
    fun oneHeavyMorningMovesTheLineByATenthOfIt() {
        // The whole reason daily weighing is bearable. A four pound jump moves
        // the smoothed number by about a tenth of that.
        val series = WeightEngine.smooth(listOf(Reading(1, 80.0), Reading(2, 81.8)))

        assertThat(series.last().smoothedKg).isWithin(TOLERANCE).of(80.18)
    }

    @Test
    fun aMissedMorningIsSkippedAndNotInvented() {
        val withGap = WeightEngine.smooth(listOf(Reading(1, 80.0), Reading(9, 79.0)))

        assertThat(withGap).hasSize(2)
        assertThat(withGap.map { it.epochDay }).containsExactly(1L, 9L).inOrder()
    }

    @Test
    fun readingsOutOfOrderAreSortedBeforeSmoothing() {
        val jumbled = WeightEngine.smooth(listOf(Reading(3, 79.0), Reading(1, 80.0), Reading(2, 79.5)))
        val ordered = WeightEngine.smooth(listOf(Reading(1, 80.0), Reading(2, 79.5), Reading(3, 79.0)))

        assertThat(jumbled.map { it.smoothedKg }).isEqualTo(ordered.map { it.smoothedKg })
    }

    @Test
    fun theLineFollowsARealFallWithoutOvershooting() {
        val falling = (0..60).map { Reading(it.toLong(), 90.0 - it * 0.05) }
        val series = WeightEngine.smooth(falling)

        // It lags, which is the point, and it never goes below the readings.
        assertThat(series.last().smoothedKg).isGreaterThan(series.last().rawKg)
        assertThat(series.last().smoothedKg).isLessThan(90.0)
    }

    @Test
    fun aSmallMoveIsAboutTheSameRatherThanADirection() {
        val series = listOf(
            Smoothed(0, 80.0, 80.0),
            Smoothed(30, 79.8, 79.8),
        )
        assertThat(WeightEngine.direction(series, asOfDay = 30))
            .isEqualTo(WeightDirection.AboutTheSame)
    }

    @Test
    fun aRealMoveGetsItsDirection() {
        val down = listOf(Smoothed(0, 80.0, 80.0), Smoothed(30, 78.0, 78.0))
        val up = listOf(Smoothed(0, 78.0, 78.0), Smoothed(30, 80.0, 80.0))

        assertThat(WeightEngine.direction(down, 30)).isEqualTo(WeightDirection.ALittleLower)
        assertThat(WeightEngine.direction(up, 30)).isEqualTo(WeightDirection.ALittleHigher)
    }

    @Test
    fun withoutAMonthOfHistoryThereIsNoDirectionToGive() {
        // Null rather than "about the same", because the app should say nothing
        // instead of saying something about a month nobody measured.
        val series = listOf(Smoothed(28, 80.0, 80.0), Smoothed(30, 79.9, 79.9))

        assertThat(WeightEngine.direction(series, asOfDay = 30)).isNull()
    }

    @Test
    fun theBandIsSymmetric() {
        val justInside = WeightEngine.SAME_BAND_KG - 0.01
        val down = listOf(Smoothed(0, 80.0, 80.0), Smoothed(30, 80.0 - justInside, 80.0 - justInside))
        val up = listOf(Smoothed(0, 80.0, 80.0), Smoothed(30, 80.0 + justInside, 80.0 + justInside))

        assertThat(WeightEngine.direction(down, 30)).isEqualTo(WeightDirection.AboutTheSame)
        assertThat(WeightEngine.direction(up, 30)).isEqualTo(WeightDirection.AboutTheSame)
    }

    @Test
    fun theNextValueMatchesTheWholeSeries() {
        val readings = listOf(Reading(1, 80.0), Reading(2, 81.0), Reading(3, 79.5))
        val series = WeightEngine.smooth(readings)

        var running: Double? = null
        readings.forEach { running = WeightEngine.next(running, it.kg) }

        assertThat(abs(running!! - series.last().smoothedKg)).isLessThan(TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.001
    }
}
