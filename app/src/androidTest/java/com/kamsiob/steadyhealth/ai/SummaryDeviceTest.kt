package com.kamsiob.steadyhealth.ai

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.data.DatabaseKey
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.VisitRepository
import com.kamsiob.steadyhealth.data.entity.CheckEntity
import com.kamsiob.steadyhealth.data.entity.CheckInEntity
import com.kamsiob.steadyhealth.data.entity.CheckInTagEntity
import com.kamsiob.steadyhealth.data.entity.ItemRatingEntity
import com.kamsiob.steadyhealth.data.entity.MeasureResultEntity
import com.kamsiob.steadyhealth.data.entity.SessionEntity
import com.kamsiob.steadyhealth.data.entity.TrackedItemEntity
import com.kamsiob.steadyhealth.data.entity.WeighInEntity
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.engine.Measures
import com.kamsiob.steadyhealth.engine.VisitSummaryEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import kotlin.math.abs

/**
 * A visit summary written from six months of real rows.
 *
 * MASTER_SPEC section 10 asks for exactly this, with exactly this assertion:
 * "asserting that every numeral on the rendered page appears in the brief". It is
 * on a device because what is being tested is the repository reading rows out of
 * an encrypted database, which is where a summary could quietly go wrong in a way
 * no JVM test would see.
 *
 * Its own database and its own key, like every other device test here.
 */
class SummaryDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db by lazy { SteadyDatabase.forTesting(context, NAME, DatabaseKey.TEST_ALIAS) }

    @After
    fun tidyUp() {
        db.close()
        SteadyDatabase.destroyTesting(context, NAME, DatabaseKey.TEST_ALIAS)
    }

    @Test
    fun everyNumeralInTheSummaryComesFromTheBrief() = runBlocking {
        seedSixMonths()
        val inputs = inputs()
        assertThat(VisitSummaryEngine.enough(inputs)).isTrue()

        val brief = VisitSummaryEngine.brief(inputs)
        val verdict = SummaryValidator.check(SummaryWriter.write(brief), brief)

        assertWithMessage("something survived to check").that(verdict.kept).isNotEmpty()
        verdict.paragraphs.forEach { paragraph ->
            assertWithMessage("${paragraph.paragraph.text} -> ${paragraph.detail}")
                .that(paragraph.faults)
                .isEmpty()
        }

        // The assertion MASTER_SPEC names, stated directly rather than left to
        // the validator: every numeral, against every number the brief holds.
        val allowed = brief.facts.flatMap { it.numbers }
        verdict.kept.forEach { paragraph ->
            SummaryValidator.digitsIn(paragraph.text).forEach { number ->
                assertWithMessage("$number in \"${paragraph.text}\"")
                    .that(allowed.any { abs(it - number) < CLOSE })
                    .isTrue()
            }
        }
    }

    @Test
    fun theNumbersSurviveWhenEveryParagraphFails() = runBlocking {
        seedSixMonths()
        val brief = VisitSummaryEngine.brief(inputs())
        // What a model that had reached past the data would send back.
        val bad = VisitSummary(
            paragraphs = List(THREE) {
                SummaryParagraph("Get up improved because of the walks.", listOf("ability_get_up"))
            },
            questions = emptyList(),
            fromModel = true,
        )
        val verdict = SummaryValidator.check(bad, brief)
        assertThat(verdict.allFailed).isTrue()
        assertThat(verdict.kept).isEmpty()
        // The measures are unaffected, which is what the fallback line promises.
        assertThat(brief.facts.filter { it.kind == "measure" }).isNotEmpty()
    }

    @Test
    fun withTooLittleBehindItNoSummaryIsWritten() = runBlocking {
        db.weighIns().upsert(weighIn(FIRST_DAY, 90.0))
        assertThat(VisitSummaryEngine.enough(inputs())).isFalse()
    }

    private suspend fun inputs() = VisitRepository(db).inputs(
        window = VisitWindow(FIRST_DAY, LAST_DAY, "March to September"),
        abilityNames = AbilityDomain.entries.associateWith { it.id },
        measureNames = Measures.all.associate { it.id to it.id },
        stateNames = AbilityState.entries.associateWith { it.id },
    )

    /** Six months: weigh-ins, check-ins with tags, sessions, and two checks. */
    private suspend fun seedSixMonths() {
        (0 until MONTHS).forEach { month ->
            val day = FIRST_DAY + month * DAYS_IN_MONTH
            db.weighIns().upsert(weighIn(day, 90.0 - month))
            val id = db.checkIns().upsert(
                CheckInEntity(
                    epochDay = day,
                    recordedAt = day * MILLIS_PER_DAY,
                    sentence = "Knees ached after the long walk.",
                    sleepHalfHours = 14,
                    dayRating = "okay",
                    spoken = false,
                ),
            )
            db.checkIns().insertTags(listOf(CheckInTagEntity(id, "sore", false)))
            db.sessions().upsert(
                SessionEntity(
                    epochDay = day,
                    startedAt = day * MILLIS_PER_DAY,
                    endedAt = day * MILLIS_PER_DAY,
                    durationSeconds = SESSION_SECONDS,
                    ladder = "walking",
                    stepIndex = month,
                    talkTest = "yes_easily",
                    steps = null,
                    distanceMetres = null,
                    nextDayFeel = null,
                    nextDayAskedOnDay = null,
                ),
            )
        }

        val item = db.abilities().upsertItem(
            TrackedItemEntity(
                text = "Carry the groceries in one trip",
                domain = AbilityDomain.Carry.id,
                createdAt = FIRST_DAY * MILLIS_PER_DAY,
                archivedAt = null,
            ),
        )
        db.abilities().upsertRating(ItemRatingEntity(item, FIRST_DAY, THREE, 0))
        db.abilities().upsertRating(ItemRatingEntity(item, LAST_DAY, SEVEN, 0))

        listOf(FIRST_DAY to NINE, LAST_DAY to FOURTEEN).forEach { (day, value) ->
            val checkId = db.checks().upsertCheck(
                CheckEntity(epochDay = day, completedAt = 0, gettingAround = "on_feet"),
            )
            db.checks().upsertMeasure(
                MeasureResultEntity(
                    checkId = checkId,
                    measureId = Measures.chairStand.id,
                    domain = AbilityDomain.GetUp.id,
                    epochDay = day,
                    recordedAt = 0,
                    value = value,
                    countedBy = "ByMotion",
                ),
            )
        }
    }

    private fun weighIn(day: Long, kg: Double) = WeighInEntity(
        epochDay = day,
        recordedAt = day * MILLIS_PER_DAY,
        rawKg = kg,
        smoothedKg = kg,
        source = "entered",
    )

    private companion object {
        const val NAME = "steady-summary-test.db"
        const val FIRST_DAY = 20_150L
        const val LAST_DAY = 20_330L
        const val MONTHS = 6
        const val DAYS_IN_MONTH = 30
        const val MILLIS_PER_DAY = 86_400_000L
        const val SESSION_SECONDS = 600
        const val CLOSE = 0.051
        const val THREE = 3
        const val SEVEN = 7
        const val NINE = 9.0
        const val FOURTEEN = 14.0
    }
}
