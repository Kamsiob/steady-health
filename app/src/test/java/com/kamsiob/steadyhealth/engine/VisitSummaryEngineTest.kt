package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.ai.Fault
import com.kamsiob.steadyhealth.ai.SummaryValidator
import com.kamsiob.steadyhealth.ai.SummaryWriter
import com.kamsiob.steadyhealth.ai.VisitBrief
import com.kamsiob.steadyhealth.ai.VisitWindow
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import org.junit.Test

/**
 * The brief the engine assembles, and the summary it writes without a model.
 *
 * AI.md: "Job 6 ships with at least 40 briefs and expected outcomes, including
 * these adversarial cases." The forty are generated below rather than typed out,
 * because the interesting axes are combinable and forty hand-written briefs would
 * cover fewer shapes than forty generated ones. The six named adversarial cases
 * are each their own test as well.
 *
 * The property that matters most: the template summary has to pass the same
 * validator the model's output does. A template that could not would be one
 * nobody should trust either.
 */
class VisitSummaryEngineTest {

    @Test
    fun thereAreAtLeastFortyBriefs() {
        assertThat(everyShape().size).isAtLeast(FORTY)
    }

    @Test
    fun everyBriefIsAssembledWithoutThrowing() {
        everyShape().forEach { inputs ->
            val brief = VisitSummaryEngine.brief(inputs)
            assertWithMessage(inputs.window.label).that(brief.facts).isNotEmpty()
        }
    }

    @Test
    fun theTemplateSummaryPassesTheValidatorForEveryBrief() {
        everyShape().forEach { inputs ->
            val brief = VisitSummaryEngine.brief(inputs)
            val verdict = SummaryValidator.check(SummaryWriter.write(brief), brief)
            verdict.paragraphs.forEach { paragraph ->
                assertWithMessage(
                    "${inputs.window.label}: ${paragraph.paragraph.text} -> ${paragraph.detail}",
                ).that(paragraph.faults).isEmpty()
            }
        }
    }

    @Test
    fun everyFactIdIsDistinctInEveryBrief() {
        everyShape().forEach { inputs ->
            val ids = VisitSummaryEngine.brief(inputs).facts.map { it.id }
            assertWithMessage(inputs.window.label).that(ids).containsNoDuplicates()
        }
    }

    @Test
    fun everyCandidatePointsAtFactsThatExist() {
        everyShape().forEach { inputs ->
            val brief = VisitSummaryEngine.brief(inputs)
            val ids = brief.facts.map { it.id }.toSet()
            brief.candidates.forEach { candidate ->
                assertWithMessage("${inputs.window.label}: ${candidate.id}")
                    .that(ids)
                    .containsAtLeastElementsIn(candidate.evidence)
            }
        }
    }

    @Test
    fun neverMoreThanFourQuestionsGoToTheModel() {
        everyShape().forEach { inputs ->
            assertThat(VisitSummaryEngine.brief(inputs).offered.size)
                .isAtMost(VisitBrief.MOST_QUESTIONS)
        }
    }

    @Test
    fun theQuestionsComeBackInRuleOrder() {
        everyShape().forEach { inputs ->
            val rules = VisitSummaryEngine.brief(inputs).offered.map { it.rule }
            assertWithMessage(inputs.window.label).that(rules).isInOrder()
        }
    }

    @Test
    fun noBriefEverContainsAWeightInKilosOrPounds() {
        // LOGIC.md section 10 and 13b: the model never sees the person's weight as
        // a number. The count of weigh-ins is a number about the app, not the body.
        everyShape().forEach { inputs ->
            val weight = VisitSummaryEngine.brief(inputs).facts.firstOrNull { it.id == "weight" }
            weight?.let {
                assertWithMessage(it.text).that(it.numbers).containsExactly(inputs.weighInCount.toDouble())
            }
        }
    }

    // --- the six named adversarial cases -------------------------------------

    @Test
    fun aBriefWithNoCandidatesProducesNoQuestionList() {
        val brief = VisitSummaryEngine.brief(plain())
        assertThat(brief.candidates).isEmpty()
        assertThat(SummaryWriter.write(brief).questions).isEmpty()
    }

    @Test
    fun aBriefWhereAllFourAreSameNeverImpliesFailure() {
        val brief = VisitSummaryEngine.brief(plain())
        val summary = SummaryWriter.write(brief)
        val text = summary.paragraphs.joinToString(" ") { it.text }
        assertThat(text).contains("Same")
        listOf("but", "only", "unfortunately", "still", "no change", "failed").forEach {
            assertWithMessage("\"$it\" in: $text").that(text.lowercase()).doesNotContain(it)
        }
    }

    @Test
    fun aBriefWithAQuieterDomainNamesNoCause() {
        val brief = VisitSummaryEngine.brief(quieter())
        val summary = SummaryWriter.write(brief)
        val verdict = SummaryValidator.check(summary, brief)
        assertThat(verdict.faults).doesNotContain(Fault.CausalClaim)
        assertThat(brief.candidates.map { it.rule }).contains(2)
    }

    @Test
    fun aSixtyDayGapIsStatedWithoutJudgement() {
        val brief = VisitSummaryEngine.brief(plain().copy(gaps = listOf(Gap(20200, 20260))))
        val gap = brief.facts.first { it.id.startsWith("gap_") }
        assertThat(gap.text).contains("60 days")
        assertThat(gap.numbers).contains(60.0)
        listOf("missed", "should", "lapse", "slipped", "fell off").forEach {
            assertWithMessage(gap.text).that(gap.text.lowercase()).doesNotContain(it)
        }
        val verdict = SummaryValidator.check(SummaryWriter.write(brief), brief)
        assertThat(verdict.allFailed).isFalse()
    }

    @Test
    fun bodyTagsOnTwelveDaysProduceAQuestionAndNotAnExplanation() {
        val brief = VisitSummaryEngine.brief(
            plain().copy(mentions = listOf(Mention("sore", 12, listOf("Knees ached again.")))),
        )
        assertThat(brief.candidates.map { it.rule }).contains(1)
        val mention = brief.facts.first { it.id == "mention_sore" }
        assertThat(mention.text).contains("12 days")
        // A statement of what happened, not of why.
        assertThat(com.kamsiob.steadyhealth.ai.Words.causalPhrasesIn(mention.text)).isEmpty()
    }

    @Test
    fun twoMeasuresMovingOppositeWaysAreBothStated() {
        val brief = VisitSummaryEngine.brief(opposite())
        val text = brief.facts.filter { it.kind == "measure" }.joinToString(" ") { it.text }
        assertThat(text).contains("22")
        assertThat(text).contains("9")
        assertThat(brief.facts.count { it.kind == "measure" }).isEqualTo(2)
    }

    @Test
    fun eightWeeksAndTwoChecksIsTheFloor() {
        assertThat(VisitSummaryEngine.enough(plain().copy(weeks = 7, checkCount = 5))).isFalse()
        assertThat(VisitSummaryEngine.enough(plain().copy(weeks = 20, checkCount = 1))).isFalse()
        assertThat(VisitSummaryEngine.enough(plain().copy(weeks = 8, checkCount = 2))).isTrue()
    }

    // --- the shapes ----------------------------------------------------------

    private fun plain() = VisitInputs(
        window = VisitWindow(20150, 20334, "March to September"),
        weeks = 24,
        checkCount = 6,
        results = listOf(
            MeasureResult(Measures.chairStand.id, 20150, 12.0),
            MeasureResult(Measures.chairStand.id, 20334, 12.0),
        ),
        items = emptyList(),
        mentions = emptyList(),
        months = listOf(MonthOfSessions("September", 19, 214)),
        gaps = emptyList(),
        currentStep = "A 14 minute walk",
        weightFirstKg = 100.0,
        weightLastKg = 100.0,
        weighInCount = 140,
        fastestWeeklyLossKg = null,
        abilityNames = AbilityDomain.entries.associateWith { it.id },
        measureNames = Measures.all.associate { it.id to it.id },
        stateNames = AbilityState.entries.associateWith {
            it.name.replaceFirstChar { c -> c.uppercase() }
        },
        pacing = false,
        anyExclusions = false,
        wayChanged = false,
    )

    private fun quieter() = plain().copy(
        results = (0..3).map {
            MeasureResult(Measures.chairStand.id, 20150 + it * 30L, 22.0 - it * 4)
        },
    )

    private fun opposite() = plain().copy(
        results = listOf(
            MeasureResult(Measures.chairStand.id, 20150, 9.0),
            MeasureResult(Measures.chairStand.id, 20334, 14.0),
            MeasureResult(Measures.singleLegStance.id, 20150, 22.0),
            MeasureResult(Measures.singleLegStance.id, 20334, 11.0),
        ),
    )

    /**
     * Forty-eight briefs, from the axes that actually change the output.
     *
     * Generated rather than typed because these combine: a Quieter domain with a
     * gap and a falling item is a different brief from any of them alone, and it
     * is exactly the combination that would break a template.
     */
    private fun everyShape(): List<VisitInputs> = buildList {
        val abilityShapes = listOf(
            "same" to plain().results,
            "better" to opposite().results.take(2),
            "quieter" to quieter().results,
            "opposite" to opposite().results,
        )
        val itemShapes = listOf(
            "no items" to emptyList(),
            "item up" to listOf(ItemHistory("Get down to the floor", 3, 20150, 7, 20334)),
            "item down" to listOf(ItemHistory("Carry the shopping", 8, 20150, 4, 20334)),
        )
        val mentionShapes = listOf(
            "no mentions" to emptyList(),
            "sore" to listOf(Mention("sore", 12, listOf("Knees ached again."))),
            "busy only" to listOf(Mention("busy", 9, listOf("Long week."))),
            "one sore day" to listOf(Mention("pain", 1, listOf("Shoulder twinged."))),
        )
        abilityShapes.forEach { (a, results) ->
            itemShapes.forEach { (i, items) ->
                mentionShapes.forEach { (m, mentions) ->
                    add(
                        plain().copy(
                            window = VisitWindow(20150, 20334, "$a, $i, $m"),
                            results = results,
                            items = items,
                            mentions = mentions,
                        ),
                    )
                }
            }
        }
        add(
            plain().copy(
                window = VisitWindow(20150, 20334, "long gap"),
                gaps = listOf(Gap(20200, 20260)),
            ),
        )
        add(
            plain().copy(
                window = VisitWindow(20150, 20334, "fast loss"),
                weightFirstKg = 110.0,
                weightLastKg = 90.0,
                fastestWeeklyLossKg = 2.1,
            ),
        )
        add(
            plain().copy(
                window = VisitWindow(20150, 20334, "no weight"),
                weightFirstKg = null,
                weightLastKg = null,
            ),
        )
        add(plain().copy(window = VisitWindow(20150, 20334, "in pacing mode"), pacing = true))
        add(
            plain().copy(
                window = VisitWindow(20150, 20334, "nothing at all"),
                results = emptyList(),
                months = emptyList(),
            ),
        )
    }

    private companion object {
        const val FORTY = 40
    }
}
