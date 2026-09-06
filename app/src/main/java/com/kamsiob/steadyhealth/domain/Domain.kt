package com.kamsiob.steadyhealth.domain

/**
 * The four abilities, from MASTER_SPEC section 4.
 *
 * Every measure, exercise, tracked item and sentence in the app belongs to
 * exactly one of these. There is deliberately no fifth value and no aggregate:
 * MASTER_SPEC is explicit that a combined score would rebuild the scoreboard the
 * app exists to remove, so there is nowhere in the code to put one.
 */
enum class AbilityDomain(val id: String) {
    GetUp("get_up"),
    Go("go"),
    Carry("carry"),
    Steady("steady"),
    ;

    companion object {
        fun fromId(id: String): AbilityDomain? = entries.firstOrNull { it.id == id }
        val ids: List<String> = entries.map { it.id }
    }
}

/**
 * Better, Same, or Quieter, from LOGIC.md 3b.
 *
 * Same carries the same weight as Better everywhere it is shown, which is a rule
 * about rendering and not only about wording: strength falls between 1.5 and 5
 * percent a year from midlife if nothing is done, so holding a number is the work.
 * Quieter is the word the app uses because "decline" is banned.
 */
enum class AbilityState(val id: String) {
    Better("better"),
    Same("same"),
    Quieter("quieter"),
    ;

    companion object {
        fun fromId(id: String): AbilityState? = entries.firstOrNull { it.id == id }
    }
}

/**
 * How a person gets around, from LOGIC.md 3b.
 *
 * It selects which measures, exercises and tile sentences exist. The four
 * domains are constant across all four; only their contents change. Nothing in
 * the interface ever indicates that one of these is better than another.
 */
enum class GettingAround(val id: String) {
    OnFeet("on_feet"),
    Walker("walker"),
    Wheelchair("wheelchair"),
    InBed("in_bed"),
    ;

    companion object {
        fun fromId(id: String): GettingAround? = entries.firstOrNull { it.id == id }
    }
}

/** The five ladders of LOGIC.md section 5. */
enum class Ladder(val id: String) {
    Walking("walking"),
    ChairAndStanding("chair_standing"),
    Floor("floor"),
    Pushing("pushing"),
    Balance("balance"),
    ;

    companion object {
        fun fromId(id: String): Ladder? = entries.firstOrNull { it.id == id }
    }
}

/** The talk test, asked after every session. LOGIC.md section 6. */
enum class TalkTest(val id: String) {
    YesEasily("yes_easily"),
    JustAbout("just_about"),
    No("no"),
    ;

    companion object {
        fun fromId(id: String): TalkTest? = entries.firstOrNull { it.id == id }
    }
}

/** Asked once, the day after a session above the current step. */
enum class NextDayFeel(val id: String) {
    Better("better"),
    Same("same"),
    Worse("worse"),
    ;

    companion object {
        fun fromId(id: String): NextDayFeel? = entries.firstOrNull { it.id == id }
    }
}

/** How the day felt. Three answers, and none of them is a grade. */
enum class DayRating(val id: String) {
    Rough("rough"),
    Okay("okay"),
    Good("good"),
    ;

    companion object {
        fun fromId(id: String): DayRating? = entries.firstOrNull { it.id == id }
    }
}

/** Where a weigh-in came from. */
enum class WeightSource(val id: String) {
    Entered("entered"),
    HealthConnect("health_connect"),
}

/** Capability answers, LOGIC.md section 4 point 3. */
enum class ChairEase(val id: String) { Hard("hard"), Okay("okay"), Easy("easy") }

enum class Stairs(val id: String) { Avoid("avoid"), OneFlightWithStop("one_flight"), Fine("fine") }

enum class WalkTolerance(val id: String) {
    UnderFive("under_5"),
    FiveToFifteen("5_to_15"),
    OverFifteen("over_15"),
}

enum class FloorAccess(val id: String) { No("no"), WithHelp("with_help"), Yes("yes") }

/** The one pattern question. Yes routes to pacing mode. */
enum class PemAnswer(val id: String) { No("no"), Sometimes("sometimes"), Yes("yes") }

/**
 * Exclusions, in movement terms only, from LOGIC.md section 4 point 4.
 *
 * The app never stores a diagnosis, never infers one, and never explains an
 * exclusion back to the person. These are the only words it has for the subject.
 */
enum class Exclusion(val id: String) {
    Pushing("pushing"),
    StomachStrain("stomach_strain"),
    GettingOnTheFloor("floor"),
    Impact("impact"),
    DeepKneeBending("deep_knee"),
    LiftingOverhead("overhead"),
    TwistingBack("twisting"),
    DeepForwardBending("forward_bend"),
    ArchingBack("arching"),
    LyingFlat("lying_flat"),
    BreathHolding("breath_holding"),
    ;

    companion object {
        fun fromId(id: String): Exclusion? = entries.firstOrNull { it.id == id }
    }
}

/** Readiness flags, original wording, never the PAR-Q+ itself. COMPLIANCE.md. */
enum class ReadinessFlag(val id: String) {
    ChestPain("chest_pain"),
    Fainting("fainting"),
    SupervisedOnly("supervised_only"),
    Pregnant("pregnant"),
    BoneOrJoint("bone_or_joint"),
    ;

    companion object {
        fun fromId(id: String): ReadinessFlag? = entries.firstOrNull { it.id == id }
    }
}

/** The habit a walk is tied to, from ONBOARDING.md screen 8. */
enum class Anchor(val id: String) {
    MorningCoffee("morning_coffee"),
    Lunch("lunch"),
    Dinner("dinner"),
    BrushingTeeth("brushing_teeth"),
    WalkingTheDog("walking_the_dog"),
    SchoolRun("school_run"),
    ;

    companion object {
        fun fromId(id: String): Anchor? = entries.firstOrNull { it.id == id }
    }
}

/** Pounds and feet, or kilos and centimetres. */
enum class Units(val id: String) { Imperial("imperial"), Metric("metric") }
