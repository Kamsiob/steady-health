package com.kamsiob.steadyhealth.session

import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround

/** How a movement is measured, which decides what the live screen shows. */
enum class Counted {
    /** Repetitions, counted up to a target. */
    Reps,

    /** A hold, in seconds, counted up to a target. */
    Hold,

    /** Minutes of walking or wheeling. Shows elapsed time and steps. */
    Minutes,

    /** Breaths, or anything else the person taps once each. */
    Taps,
}

/** What the session is made of, in the order a session uses them. */
enum class Piece { WarmUp, Main, CoolDown }

/**
 * The part of the body a movement asks most of.
 *
 * Only used for one thing: when somebody presses "Something hurts" and names an area,
 * every movement with that area is suppressed for seven days. It is not a muscle
 * group and nothing else reads it.
 */
enum class Area(val id: String) {
    Shoulder("shoulder"),
    Arm("arm"),
    Back("back"),
    Hip("hip"),
    Knee("knee"),
    Ankle("ankle"),
    Neck("neck"),
    None("none"),
}

/** What a movement needs to exist in the room. */
enum class Kit(val id: String) {
    None("none"),
    Chair("chair"),
    Wall("wall"),
    Band("band"),
    Step("step"),
    Weights("weights"),
    Floor("floor"),
}

/**
 * One movement.
 *
 * Everything the app can ask somebody to do is one of these, written out in full,
 * so the whole of what the app might ask can be read in one sitting by them or by
 * anybody forking this. Nothing generates a movement and nothing adapts one beyond
 * the deterministic rules in [SessionEngine].
 *
 * [easier] and [harder] are the ladder now. Reps are the progression only up to
 * [ceiling]; past it the engine moves to [harder], which is what ADDENDUM-03 Part 1
 * means by progressing by variant.
 */
/**
 * Whether the phone can count this one on its own.
 *
 * Only for movements where the phone moves with the body: a stand lifts it a long
 * way, a step lifts it a little. Everything else is counted by the person tapping,
 * which is the honest answer rather than a sensor guessing.
 */
enum class Sensed {
    None,
    Steps,
    Stands,
}

data class Movement(
    val id: String,
    val name: String,
    /** One line, said aloud before the set starts. */
    val setup: String,
    /** When to stop, said aloud with the setup. Never a warning, always a permission. */
    val stopRule: String,
    val counted: Counted,
    val piece: Piece,
    val domain: AbilityDomain,
    val area: Area,
    val kit: Set<Kit>,
    /** Where somebody new to this movement starts. */
    val startTarget: Int,
    /** Past this, reps stop being the progression. Null for warm ups and cool downs. */
    val ceiling: Int? = null,
    val easier: String? = null,
    val harder: String? = null,
    /** The ways of getting around this movement makes sense for. */
    val ways: Set<GettingAround> = setOf(GettingAround.OnFeet, GettingAround.Walker),
    /** Anything the person left out that hides this movement. */
    val excludedBy: Set<Exclusion> = emptySet(),
    /** Roughly how long one set takes, for planning a four to eight minute session. */
    val seconds: Int = 45,
    /** Whether the accelerometer can count this without anybody tapping. */
    val sensedBy: Sensed = Sensed.None,
) {
    val isWalk: Boolean get() = counted == Counted.Minutes
}
