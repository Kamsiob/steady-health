package com.kamsiob.steadyhealth.session

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.GettingAround
import org.junit.Test

/**
 * The four versions of the app, each held to the same session.
 *
 * LOGIC.md 3b says the way somebody gets around selects which exercises exist and
 * that the four abilities are constant across all four versions. That is easy to
 * write and easy to half do: the library grew up on feet, and a movement written for
 * somebody sitting down is one field away from never being offered to anybody sitting
 * down. So this runs a fortnight of consecutive sessions for each of the four and
 * asks the same questions of every one of them.
 *
 * A fortnight rather than a day, because the interesting failures are the ones that
 * need history: an ability the engine never reaches, a movement offered two days
 * running, a walk long enough to make one session three times the length of the next.
 * None of those show up in a single plan.
 */
class EveryWayTest {

    @Test
    fun everyWayGetsAWholeSessionEveryDayForAFortnight() {
        GettingAround.entries.forEach { way ->
            fortnight(way).forEachIndexed { day, plan ->
                val where = "${way.id}, day $day"
                assertWithMessage("$where is empty").that(plan.steps).isNotEmpty()
                assertWithMessage("$where has no warm up").that(plan.hasWarmUp).isTrue()
                assertWithMessage("$where has too few main movements")
                    .that(plan.main.size)
                    .isAtLeast(SessionEngine.MAIN_MOVEMENTS)
                assertWithMessage("$where has no cool down").that(plan.hasCoolDown).isTrue()
            }
        }
    }

    @Test
    fun everySessionIsBetweenFourAndEightMinutes() {
        GettingAround.entries.forEach { way ->
            fortnight(way).forEachIndexed { day, plan ->
                val where = "${way.id}, day $day: ${plan.minutes} minutes"
                assertWithMessage(where).that(plan.minutes).isAtLeast(SHORTEST_MINUTES)
                assertWithMessage(where).that(plan.minutes).isAtMost(SessionEngine.LONGEST_MINUTES)
            }
        }
    }

    @Test
    fun nobodyIsEverOfferedAMovementTaggedForSomebodyElse() {
        GettingAround.entries.forEach { way ->
            fortnight(way).forEach { plan ->
                plan.steps.forEach { step ->
                    assertWithMessage("${step.movement.id} was offered to ${way.id}")
                        .that(step.movement.ways)
                        .contains(way)
                }
            }
        }
    }

    @Test
    fun aWeekOfSessionsReachesAllFourAbilities() {
        // Both weeks separately, because an ability that turns up once on day two and
        // never again is not the four abilities being constant across the versions.
        GettingAround.entries.forEach { way ->
            fortnight(way).chunked(A_WEEK).forEachIndexed { week, days ->
                val fed = days.flatMap { plan -> plan.main.map { it.movement.domain } }.toSet()
                assertWithMessage("${way.id}, week $week")
                    .that(fed)
                    .containsExactlyElementsIn(AbilityDomain.entries)
            }
        }
    }

    @Test
    fun nothingIsOfferedTwoDaysRunning() {
        GettingAround.entries.forEach { way ->
            fortnight(way).map { plan -> plan.steps.map { it.movement.id } }
                .zipWithNext()
                .forEachIndexed { day, (yesterday, today) ->
                    assertWithMessage("${way.id}, day ${day + 1} repeats")
                        .that(today.filter { it in yesterday })
                        .isEmpty()
                }
        }
    }

    @Test
    fun nothingIsOfferedTwiceInTheSameSession() {
        GettingAround.entries.forEach { way ->
            fortnight(way).forEachIndexed { day, plan ->
                assertWithMessage("${way.id}, day $day")
                    .that(plan.steps.map { it.movement.id })
                    .containsNoDuplicates()
            }
        }
    }

    @Test
    fun everyWayHasEnoughOfEveryAbilityToRotate() {
        // The reason nothing repeats two days running is that there is something else
        // to offer instead. Held here so a way that quietly drops to one movement in an
        // ability fails on the count rather than on a day fourteen sessions later.
        GettingAround.entries.forEach { way ->
            AbilityDomain.entries.forEach { domain ->
                val mine = Movements.available(way).filter {
                    it.piece == Piece.Main && it.domain == domain
                }
                assertWithMessage("${way.id} has ${mine.size} movements for ${domain.id}")
                    .that(mine.size)
                    .isAtLeast(ENOUGH_TO_ROTATE)
            }
        }
    }

    @Test
    fun everyWayHasAWarmUpAndACoolDownToRotateThrough() {
        GettingAround.entries.forEach { way ->
            val mine = Movements.available(way)
            assertWithMessage("${way.id} warm ups")
                .that(mine.count { it.piece == Piece.WarmUp })
                .isAtLeast(ENOUGH_TO_ROTATE)
            // Two are used every day, so four is what it takes to go a day without
            // repeating either of them.
            assertWithMessage("${way.id} cool downs")
                .that(mine.count { it.piece == Piece.CoolDown })
                .isAtLeast(ENOUGH_TO_ROTATE + ENOUGH_TO_ROTATE)
        }
    }

    @Test
    fun theSameFortnightAlwaysPlansTheSameWay() {
        GettingAround.entries.forEach { way ->
            val once = fortnight(way).map { plan -> plan.steps.map { it.movement.id } }
            val again = fortnight(way).map { plan -> plan.steps.map { it.movement.id } }
            assertThat(once).isEqualTo(again)
        }
    }

    /**
     * Fourteen consecutive days, each planned from what the days before it did.
     *
     * Everything is answered "about right", so nothing here is testing the easy day or
     * the lighter session: what is left is the plain shape of a session, which is the
     * part that has to hold for all four ways.
     */
    private fun fortnight(way: GettingAround): List<SessionPlan> {
        val history = mutableListOf<Done>()
        val plans = mutableListOf<SessionPlan>()
        repeat(A_FORTNIGHT) { day ->
            val today = FIRST_DAY + day
            val plan = SessionEngine.plan(
                SessionInputs(
                    way = way,
                    history = history.toList(),
                    today = today,
                    lastSessionDay = if (day == 0) null else today - 1,
                    lastFelt = if (day == 0) null else Felt.AboutRight,
                    feltBefore = if (day < 2) null else Felt.AboutRight,
                ),
            )
            plans += plan
            plan.steps.forEach {
                history += Done(it.movement.id, today, result = it.target, target = it.target)
            }
        }
        return plans
    }

    private companion object {
        const val FIRST_DAY = 20_000L
        const val A_WEEK = 7
        const val A_FORTNIGHT = 14
        const val SHORTEST_MINUTES = 4
        const val ENOUGH_TO_ROTATE = 2
    }
}
