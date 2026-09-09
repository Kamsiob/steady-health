package com.kamsiob.steadyhealth.session

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.engine.Returning
import com.kamsiob.steadyhealth.engine.ReturningEngine
import org.junit.Test

/**
 * The four ways of getting around, under the conditions that break them.
 *
 * [EveryWayTest] asks whether a session holds for somebody with nothing in their
 * way. This one asks whether it holds for the people the app is actually for: a
 * person who left out six things at onboarding, a person with no chair and no wall,
 * a person whose shoulder has hurt since Tuesday, a person whose limit is five
 * minutes, and a person who was on their feet in March and is in a wheelchair now.
 *
 * The bar is the same for all of them and it is not negotiable: a session that is not
 * empty, has a warm up, has three main movements, has a cool down, comes to between
 * four and eight minutes, contains nothing they said to leave out, nothing that asks
 * for equipment they do not have, nothing that uses an area that hurts, and nothing
 * written for a body other than theirs. Every failure below was a real one before the
 * assertion was written; none of them showed up in a single ordinary plan.
 */
class EveryWayHardTest {

    // --- what they said to leave out ------------------------------------------

    @Test
    fun everyExclusionOnItsOwnStillLeavesAWholeSession() {
        GettingAround.entries.forEach { way ->
            Exclusion.entries.forEach { left ->
                val out = setOf(left)
                val where = "${way.id} without ${left.id}"
                aWeek(SessionInputs(way = way, exclusions = out)).forEachIndexed { day, plan ->
                    whole(plan, way, out, ROOM, noSoreArea, "$where, day $day")
                }
            }
        }
    }

    @Test
    fun everythingExcludedAtOnceStillLeavesAWholeSession() {
        // LOGIC.md section 5: a guaranteed floor when nearly everything is excluded.
        // Including a wheelchair user who has left out pushing and lifting overhead,
        // and somebody in bed who cannot lie flat, which are the two the promise is
        // about.
        val everything = Exclusion.entries.toSet()
        GettingAround.entries.forEach { way ->
            val where = "${way.id} with everything left out"
            aWeek(SessionInputs(way = way, exclusions = everything)).forEachIndexed { day, plan ->
                whole(plan, way, everything, ROOM, noSoreArea, "$where, day $day")
            }
        }
    }

    // --- what is in the room --------------------------------------------------

    @Test
    fun everyCombinationOfEquipmentLeavesAWholeSession() {
        kits().forEach { kit ->
            GettingAround.entries.forEach { way ->
                val week = aWeek(SessionInputs(way = way, kit = kit), days = A_FEW)
                week.forEachIndexed { day, plan ->
                    whole(plan, way, noExclusions, kit, noSoreArea, "${way.id} with $kit, day $day")
                }
            }
        }
    }

    @Test
    fun nothingInTheRoomAtAllStillLeavesAWholeSession() {
        // Two answers the app itself can produce: "no" to the sturdy chair question,
        // and the bare case where the room has nothing in it at all.
        listOf(NOTHING, setOf(Kit.None, Kit.Wall)).forEach { kit ->
            GettingAround.entries.forEach { way ->
                aWeek(SessionInputs(way = way, kit = kit)).forEachIndexed { day, plan ->
                    whole(plan, way, noExclusions, kit, noSoreArea, "${way.id} with $kit, day $day")
                }
            }
        }
    }

    @Test
    fun everyWayKeepsAllFourAbilitiesWhateverIsInTheRoom() {
        // The four abilities are constant across the four ways: LOGIC.md 3b. An
        // ability with nothing in it is one the person can never be offered, and
        // answering "no" to the chair question used to empty Get up for anybody on
        // their feet, for good.
        kits().forEach { kit ->
            GettingAround.entries.forEach { way ->
                AbilityDomain.entries.forEach { ability ->
                    val mine = Movements.available(way, kit = kit)
                        .filter { it.piece == Piece.Main && it.domain == ability }
                    assertWithMessage("${way.id} with $kit has nothing for ${ability.id}")
                        .that(mine)
                        .isNotEmpty()
                }
            }
        }
    }

    // --- something hurts ------------------------------------------------------

    @Test
    fun eachSoreAreaInTurnStillLeavesAWholeSession() {
        reportableAreas.forEach { sore ->
            GettingAround.entries.forEach { way ->
                val hurts = setOf(sore)
                val where = "${way.id}, ${sore.id} hurts"
                aWeek(SessionInputs(way = way, sore = hurts)).forEachIndexed { day, plan ->
                    whole(plan, way, noExclusions, ROOM, hurts, "$where, day $day")
                }
            }
        }
    }

    @Test
    fun aSoreAreaOnTopOfEverythingElseStillLeavesAWholeSession() {
        val everything = Exclusion.entries.toSet()
        reportableAreas.forEach { sore ->
            GettingAround.entries.forEach { way ->
                val inputs = SessionInputs(
                    way = way,
                    exclusions = everything,
                    kit = NOTHING,
                    sore = setOf(sore),
                )
                val where = "${way.id}, worst case, ${sore.id} hurts"
                aWeek(inputs).forEachIndexed { day, plan ->
                    whole(plan, way, everything, NOTHING, setOf(sore), "$where, day $day")
                }
            }
        }
    }

    @Test
    fun twoSoreAreasAtOnceStillLeaveSomethingToOffer() {
        // Two presses of "Something hurts" inside a week is one person having a bad
        // fortnight, not an edge case. It used to leave a two movement session, and
        // an easy day with nothing in it at all.
        reportableAreas.forEach { first ->
            reportableAreas.filter { it != first }.forEach { second ->
                GettingAround.entries.forEach { way ->
                    val sore = setOf(first, second)
                    val inputs = SessionInputs(way = way, kit = NOTHING, sore = sore)
                    val where = "${way.id}, ${first.id} and ${second.id} hurt"
                    whole(SessionEngine.plan(inputs), way, noExclusions, NOTHING, sore, where)
                    val easy = SessionEngine.plan(inputs.copy(lastFelt = Felt.Hard))
                    assertWithMessage("$where, easy day").that(easy.steps).isNotEmpty()
                }
            }
        }
    }

    @Test
    fun aSessionKeepsThreeMovementsWhenOnlyTwoAbilitiesHaveAnythingLeft() {
        // The exact combinations that used to hand somebody a two movement session
        // while a dozen movements they could have done sat unused. One movement per
        // ability is how a session covers the four; it was never meant to be a cap.
        listOf(
            SessionInputs(
                way = GettingAround.OnFeet,
                exclusions = setOf(Exclusion.DeepForwardBending),
                kit = setOf(Kit.None, Kit.Wall),
                sore = setOf(Area.Shoulder, Area.Arm),
            ),
            SessionInputs(
                way = GettingAround.InBed,
                exclusions = setOf(
                    Exclusion.Pushing,
                    Exclusion.LiftingOverhead,
                    Exclusion.LyingFlat,
                ),
                kit = setOf(Kit.None, Kit.Wall),
                sore = setOf(Area.Arm, Area.Back),
            ),
        ).forEach { inputs ->
            val main = Movements.available(inputs.way, inputs.exclusions, inputs.kit, inputs.sore)
                .filter { it.piece == Piece.Main }
            val fed = main.map { it.domain }.distinct()
            assertWithMessage("${inputs.way.id} still has ${fed.size} abilities to draw on")
                .that(fed.size)
                .isLessThan(SessionEngine.MAIN_MOVEMENTS)

            aWeek(inputs).forEachIndexed { day, plan ->
                val where = "${inputs.way.id} with two abilities left, day $day"
                whole(plan, inputs.way, inputs.exclusions, inputs.kit, inputs.sore, where)
            }
        }
    }

    // --- pacing mode ----------------------------------------------------------

    @Test
    fun pacingModeNeverPlansMoreThanTheEnvelope() {
        (SHORTEST_ENVELOPE..SessionEngine.LONGEST_MINUTES).forEach { envelope ->
            GettingAround.entries.forEach { way ->
                val inputs = SessionInputs(way = way, pacingMinutes = envelope)
                aWeek(inputs).forEachIndexed { day, plan ->
                    val where = "${way.id}, a $envelope minute limit, day $day"
                    assertWithMessage("$where is empty").that(plan.steps).isNotEmpty()
                    assertWithMessage("$where runs to ${plan.seconds} seconds")
                        .that(plan.seconds)
                        .isAtMost(envelope * SECONDS_PER_MINUTE)
                }
            }
        }
    }

    @Test
    fun pacingModeNeverAsksForMoreThanLastTime() {
        // LOGIC.md section 7: nothing increases. Two easy sessions in a row is the one
        // rule that adds a rep, and in pacing mode it is the rule that does harm.
        GettingAround.entries.forEach { way ->
            val asked = mutableMapOf<String, MutableSet<Int>>()
            aWeek(SessionInputs(way = way, pacingMinutes = AN_ENVELOPE), felt = Felt.Easy)
                .forEach { plan ->
                    plan.steps.forEach {
                        asked.getOrPut(it.movement.id) { mutableSetOf() } += it.target
                    }
                }
            asked.forEach { (id, targets) ->
                assertWithMessage("${way.id} moved the number for $id to $targets")
                    .that(targets)
                    .hasSize(1)
            }
        }
        // And the same fortnight out of pacing mode does move, so the test above is
        // saying something.
        val moved = aWeek(SessionInputs(way = GettingAround.OnFeet), felt = Felt.Easy)
            .flatMap { plan -> plan.steps.map { it.movement.id to it.target } }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.distinct().size > 1 }
        assertThat(moved).isNotEmpty()
    }

    // --- an easy day, and ninety seconds --------------------------------------

    @Test
    fun theEasyDayIsNeverBlankAndIsNeverTheSameTwiceRunning() {
        GettingAround.entries.forEach { way ->
            val history = mutableListOf<Done>()
            val days = (0 until A_WEEK).map { day ->
                val today = FIRST_DAY + day
                val plan = SessionEngine.plan(
                    SessionInputs(
                        way = way,
                        history = history.toList(),
                        today = today,
                        lastSessionDay = today - 1,
                        lastFelt = Felt.Hard,
                    ),
                )
                plan.steps.forEach {
                    history += Done(it.movement.id, today, it.target, it.target)
                }
                plan
            }
            days.forEachIndexed { day, plan ->
                assertWithMessage("${way.id} easy day $day is empty").that(plan.steps).isNotEmpty()
                assertWithMessage("${way.id} easy day $day is not easy").that(plan.easy).isTrue()
                plan.steps.forEach {
                    assertWithMessage("${way.id} easy day $day offered ${it.movement.id}")
                        .that(it.movement.ways)
                        .contains(way)
                }
            }
            assertWithMessage("${way.id} offers the same easy day every time")
                .that(days.map { plan -> plan.steps.map { it.movement.id } }.distinct().size)
                .isGreaterThan(1)

            // And the easy day somebody has on their worst week. Every warm up in the
            // library can be taken away at once by three sore areas and the exclusions,
            // and an easy day with nothing in it is still a blank screen.
            val worst = SessionEngine.plan(
                SessionInputs(
                    way = way,
                    exclusions = Exclusion.entries.toSet(),
                    kit = NOTHING,
                    sore = setOf(Area.Shoulder, Area.Arm, Area.Hip),
                    lastFelt = Felt.Hard,
                ),
            )
            assertWithMessage("${way.id} has an empty easy day at its worst")
                .that(worst.steps)
                .isNotEmpty()
        }
    }

    @Test
    fun theNinetySecondVersionAlwaysHasSomethingInIt() {
        val everything = Exclusion.entries.toSet()
        GettingAround.entries.forEach { way ->
            reportableAreas.forEach { sore ->
                val plan = SessionEngine.plan(
                    SessionInputs(
                        way = way,
                        exclusions = everything,
                        kit = NOTHING,
                        sore = setOf(sore),
                        wantSmall = true,
                    ),
                )
                val where = "${way.id}, worst case, ${sore.id} hurts, ninety seconds"
                assertWithMessage(where).that(plan.steps).hasSize(1)
                assertWithMessage(where).that(plan.steps.first().movement.ways).contains(way)
            }
        }
    }

    // --- day one, and a hundred days ------------------------------------------

    @Test
    fun dayOneHasSomethingToOfferWhateverTheyAnswered() {
        val everything = Exclusion.entries.toSet()
        GettingAround.entries.forEach { way ->
            listOf(noExclusions, everything).forEach { left ->
                listOf(ROOM, NOTHING).forEach { kit ->
                    val inputs = SessionInputs(way = way, exclusions = left, kit = kit)
                    val where = "${way.id}, day one, ${left.size} left out, $kit"
                    val first = SessionEngine.first(inputs)
                    assertWithMessage("$where has no first movement").that(first.steps).hasSize(1)
                    assertWithMessage(where).that(first.steps.first().movement.ways).contains(way)
                    whole(SessionEngine.plan(inputs), way, left, kit, noSoreArea, where)
                }
            }
        }
    }

    @Test
    fun aHundredDaysOfHistoryStillPlansAWholeSession() {
        GettingAround.entries.forEach { way ->
            // Everything easy, so every ceiling in the library is reached and the
            // engine spends most of the hundred days on the harder variants.
            val long = aWeek(SessionInputs(way = way), days = A_LONG_TIME, felt = Felt.Easy)
            long.forEachIndexed { day, plan ->
                whole(plan, way, noExclusions, ROOM, noSoreArea, "${way.id}, day $day of many")
            }
        }
    }

    // --- changing how you get around ------------------------------------------

    @Test
    fun changingHowYouGetAroundKeepsTheHistoryAndStillPlansASession() {
        listOf(
            GettingAround.OnFeet to GettingAround.Wheelchair,
            GettingAround.InBed to GettingAround.OnFeet,
            GettingAround.Walker to GettingAround.InBed,
        ).forEach { (before, after) ->
            val history = mutableListOf<Done>()
            aWeek(SessionInputs(way = before), days = A_MONTH, into = history)
            val asItWas = history.toList()

            val plans = aWeek(
                SessionInputs(way = after),
                days = A_MONTH,
                from = FIRST_DAY + A_MONTH,
                into = history,
            )
            val where = "${after.id} after a month ${before.id}"
            plans.forEachIndexed { day, plan ->
                whole(plan, after, noExclusions, ROOM, noSoreArea, "$where, day $day")
            }
            // A wheeling session stays a wheeling session afterwards: nothing that was
            // done before the change is rewritten, removed, or re-read as something
            // else, and every row still names a movement the app knows.
            assertWithMessage("${before.id} to ${after.id} lost history")
                .that(history)
                .containsAtLeastElementsIn(asItWas)
            history.forEach {
                assertWithMessage("history names ${it.movementId}, which is not in the library")
                    .that(Movements.byId(it.movementId))
                    .isNotNull()
            }
        }
    }

    @Test
    fun aMovementBothWaysShareKeepsItsNumberAcrossTheChange() {
        val history = mutableListOf<Done>()
        val onTheirFeet = SessionInputs(way = GettingAround.OnFeet)
        aWeek(onTheirFeet, days = A_MONTH, felt = Felt.Easy, into = history)
        val done = history.map { it.movementId }.toSet()
        val shared = Movements.available(GettingAround.OnFeet)
            .filter { GettingAround.Wheelchair in it.ways && it.id in done }
        assertThat(shared).isNotEmpty()

        shared.forEach { movement ->
            val onFeet = SessionInputs(
                way = GettingAround.OnFeet,
                history = history.toList(),
                today = FIRST_DAY + A_MONTH,
                lastSessionDay = FIRST_DAY + A_MONTH - 1,
            )
            val wheeling = onFeet.copy(way = GettingAround.Wheelchair)
            val lastAsked = history.filter { it.movementId == movement.id }
                .maxByOrNull { it.epochDay }
                ?.target
            assertWithMessage("${movement.id} forgot its number when the way changed")
                .that(SessionEngine.stepFor(movement, wheeling).target)
                .isEqualTo(lastAsked)
            assertWithMessage("${movement.id} reads differently for the two ways")
                .that(SessionEngine.stepFor(movement, wheeling).target)
                .isEqualTo(SessionEngine.stepFor(movement, onFeet).target)
        }
    }

    // --- a long time away -----------------------------------------------------

    @Test
    fun aGapOfMonthsStillPlansAWholeSessionTheDayTheyComeBack() {
        GettingAround.entries.forEach { way ->
            val history = mutableListOf<Done>()
            aWeek(SessionInputs(way = way), days = A_MONTH, into = history)
            val backAgain = FIRST_DAY + A_MONTH + A_LONG_GAP
            val plan = SessionEngine.plan(
                SessionInputs(
                    way = way,
                    history = history.toList(),
                    today = backAgain,
                    lastSessionDay = FIRST_DAY + A_MONTH - 1,
                    lastFelt = Felt.AboutRight,
                ),
            )
            val where = "${way.id} back after $A_LONG_GAP days"
            whole(plan, way, noExclusions, ROOM, noSoreArea, where)
            assertWithMessage("${way.id} came back to the same numbers as before")
                .that(plan.adaptation.kind)
                .isEqualTo(Adaptation.Kind.Shorter)
        }
    }

    @Test
    fun comingBackAfterMonthsAsksHowYouGetAroundAgain() {
        // LOGIC.md 3b re-asks the way after a gap over ninety days; ADDENDUM-03 Part
        // 15 re-runs O2 after sixty. Nothing asked either question before this.
        val short = ReturningEngine.decide(lastSessionDay = DAY, today = DAY + A_MONTH)
        assertThat(short).isInstanceOf(Returning.AfterAGap::class.java)
        assertThat((short as Returning.AfterAGap).askTheWayAgain).isFalse()

        val months = ReturningEngine.decide(
            lastSessionDay = DAY,
            today = DAY + ReturningEngine.ASK_THE_WAY_AGAIN_DAYS,
        ) as Returning.AfterAGap
        assertThat(months.askTheWayAgain).isTrue()

        val ninety = ReturningEngine.decide(
            lastSessionDay = DAY,
            today = DAY + ReturningEngine.ASK_AGAIN_DAYS + 1,
        ) as Returning.AfterAGap
        assertThat(ninety.askTheWayAgain).isTrue()
        assertThat(ninety.askAgain).isTrue()
    }

    // --- and the same answer twice --------------------------------------------

    @Test
    fun theSameHardWeekAlwaysPlansTheSameWay() {
        GettingAround.entries.forEach { way ->
            val inputs = SessionInputs(
                way = way,
                exclusions = Exclusion.entries.toSet(),
                kit = NOTHING,
                sore = setOf(Area.Shoulder),
            )
            val once = aWeek(inputs).map { plan -> plan.steps.map { it.movement.id } }
            val again = aWeek(inputs).map { plan -> plan.steps.map { it.movement.id } }
            assertWithMessage(way.id).that(once).isEqualTo(again)
        }
    }

    // --- the bar every session is held to -------------------------------------

    private fun whole(
        plan: SessionPlan,
        way: GettingAround,
        exclusions: Set<Exclusion>,
        kit: Set<Kit>,
        sore: Set<Area>,
        where: String,
    ) {
        assertWithMessage("$where is empty").that(plan.steps).isNotEmpty()
        assertWithMessage("$where has no main movements").that(plan.main).isNotEmpty()
        assertWithMessage("$where has ${plan.main.size} main movements")
            .that(plan.main.size)
            .isAtLeast(SessionEngine.MAIN_MOVEMENTS)
        assertWithMessage("$where has no warm up").that(plan.hasWarmUp).isTrue()
        assertWithMessage("$where has no cool down").that(plan.hasCoolDown).isTrue()
        assertWithMessage("$where runs to ${plan.minutes} minutes")
            .that(plan.minutes)
            .isAtLeast(SHORTEST_MINUTES)
        assertWithMessage("$where runs to ${plan.minutes} minutes")
            .that(plan.minutes)
            .isAtMost(SessionEngine.LONGEST_MINUTES)
        assertWithMessage("$where repeats itself")
            .that(plan.steps.map { it.movement.id })
            .containsNoDuplicates()
        plan.steps.forEach { step ->
            val movement = step.movement
            assertWithMessage("$where offered ${movement.id}, written for somebody else")
                .that(movement.ways)
                .contains(way)
            assertWithMessage("$where offered ${movement.id}, which they left out")
                .that(movement.excludedBy.filter { it in exclusions })
                .isEmpty()
            assertWithMessage("$where offered ${movement.id}, which needs ${movement.kit}")
                .that(movement.kit.filter { it != Kit.None && it !in kit })
                .isEmpty()
            assertWithMessage("$where offered ${movement.id}, and ${movement.area.id} hurts")
                .that(movement.area)
                .isNotIn(sore)
            assertWithMessage("$where asked for ${step.target} of ${movement.id}")
                .that(step.target)
                .isAtLeast(1)
        }
    }

    /**
     * Consecutive days, each planned from what the days before it did.
     *
     * The same shape as the fortnight in [EveryWayTest], with the inputs given rather
     * than assumed, so one helper covers a person with nothing in the room, a person
     * with a limit, and a person changing how they get around.
     */
    private fun aWeek(
        inputs: SessionInputs,
        days: Int = A_WEEK,
        felt: Felt = Felt.AboutRight,
        from: Long = FIRST_DAY,
        into: MutableList<Done> = mutableListOf(),
    ): List<SessionPlan> = (0 until days).map { day ->
        val today = from + day
        val started = into.isEmpty() && day == 0
        val plan = SessionEngine.plan(
            inputs.copy(
                history = into.toList(),
                today = today,
                lastSessionDay = if (started) null else today - 1,
                lastFelt = if (started) null else felt,
                feltBefore = if (into.size < 2) null else felt,
            ),
        )
        plan.steps.forEach { into += Done(it.movement.id, today, it.target, it.target) }
        plan
    }

    /** Every combination of what a room might have in it, the empty one included. */
    private fun kits(): List<Set<Kit>> {
        val all = Kit.entries
        return (0 until (1 shl all.size)).map { mask ->
            all.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.toSet()
        }
    }

    private companion object {
        const val FIRST_DAY = 20_000L
        const val DAY = 500L
        const val A_FEW = 3
        const val A_WEEK = 7
        const val A_MONTH = 30
        const val A_LONG_TIME = 100
        const val A_LONG_GAP = 120
        const val SHORTEST_MINUTES = 4
        const val SECONDS_PER_MINUTE = 60
        const val SHORTEST_ENVELOPE = 1
        const val AN_ENVELOPE = 5

        val ROOM = setOf(Kit.None, Kit.Chair, Kit.Wall)
        val NOTHING = setOf(Kit.None)
        val noExclusions = emptySet<Exclusion>()
        val noSoreArea = emptySet<Area>()

        /** The seven the person can actually name. Area.None is never one of them. */
        val reportableAreas = Area.entries.filter { it != Area.None }
    }
}
