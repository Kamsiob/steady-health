package com.kamsiob.steadyhealth.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.data.DatabaseKey
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

/**
 * The two surfaces numbers-off gained in Phase 8, against a real database.
 *
 * NumbersOffTest covers the weight page and says in its own comment that the week
 * line and the noticed counts were still to come. They have come, and this is them.
 *
 * It is an instrumented test because the answer depends on a setting that lives in
 * an encrypted database, and the whole question is whether the sentence somebody
 * reads changes when that setting does. A pure test of the wording would pass
 * whether or not anything ever asked the setting.
 *
 * It exists because a device gate could not answer this. The gate drove the switch
 * without reading it, so it kept testing the state it had accidentally left behind,
 * and three runs said three different things. A test that owns its own database has
 * no such doubt: it writes the setting, then reads the sentence.
 */
class NumbersOffTodayTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val db by lazy { SteadyDatabase.forTesting(context, NAME, DatabaseKey.TEST_ALIAS) }

    private val cards by lazy {
        TodayCards(context.applicationContext as android.app.Application, db)
    }

    @After
    fun tidy() {
        db.close()
        SteadyDatabase.destroyTesting(context, NAME, DatabaseKey.TEST_ALIAS)
    }

    @Test
    fun theWeekLineCountsSessionsWithNumbersOn() = runBlocking {
        ProfileRepository(db).setShowNumbers(true)

        val line = cards.weekLine(TODAY)

        assertWithMessage("with numbers on the week line says how many: $line")
            .that(line.any { it.isDigit() })
            .isTrue()
    }

    @Test
    fun theWeekLineHasNoDigitInItWithNumbersOff() = runBlocking {
        ProfileRepository(db).setShowNumbers(false)

        val line = cards.weekLine(TODAY)

        assertWithMessage("the week line still counts sessions: $line")
            .that(line.filter { it.isDigit() })
            .isEmpty()
        assertThat(line).isNotEmpty()
    }

    @Test
    fun nothingTheAppNoticedHasADigitInItWithNumbersOff() = runBlocking {
        ProfileRepository(db).setShowNumbers(false)

        cards.noticedLines(TODAY).forEach { line ->
            assertWithMessage("a noticed line still carries a number: $line")
                .that(line.filter { it.isDigit() })
                .isEmpty()
        }
    }

    @Test
    fun theFourWeekBarsKeepTheirHeightAndLoseTheirLabels() = runBlocking {
        ProfileRepository(db).setShowNumbers(false)

        val bars = cards.weekBars(numbersOn = false)

        assertThat(bars).isNotEmpty()
        bars.forEach { bar ->
            assertWithMessage("a bar still prints its count: ${bar.label}")
                .that(bar.label)
                .isEmpty()
            assertWithMessage("a bar is still read aloud as a count: ${bar.spoken}")
                .that(bar.spoken.filter { it.isDigit() })
                .isNotEmpty() // the week's own number, which is which week it is
        }
    }

    private companion object {
        const val NAME = "numbers-off-today-test.db"
        const val TODAY = 20_000L
    }
}
