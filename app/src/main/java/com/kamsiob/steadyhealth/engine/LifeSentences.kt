package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.domain.AbilityDomain

/**
 * One hand-written sentence about life, and the number that earns it.
 *
 * [atLeast] is in the measure's own unit. The sentence is shown when the person's
 * latest result reaches it, and the highest one they reach is the one shown.
 */
data class LifeSentence(
    val id: String,
    val domain: AbilityDomain,
    val measureId: String,
    val atLeast: Double,
)

/**
 * The life sentence, from LOGIC.md 3b.
 *
 * "The engine picks the sentence; the model never writes one." This is the
 * engine picking it: a small table of hand-written sentences keyed to a measure
 * crossing a threshold, and nothing else. The words themselves are resources,
 * because a person reads them.
 *
 * The thresholds are not norms and are not compared to anybody. They are the
 * points where a number stops being a number and starts being a thing somebody
 * can do: fourteen chair stands is getting out of a low sofa without thinking
 * about it, twenty seconds on one foot is putting trousers on standing up.
 *
 * Where somebody has no result yet, there is no sentence, and the tile carries
 * their own words instead. That is not a gap to fill later; a made-up sentence
 * about somebody's life is worse than none.
 */
object LifeSentences {

    val all: List<LifeSentence> = listOf(
        // Get up: the chair stand, in what it buys.
        LifeSentence("get_up_low_chair", AbilityDomain.GetUp, Measures.chairStand.id, 8.0),
        LifeSentence("get_up_no_hands", AbilityDomain.GetUp, Measures.chairStand.id, 12.0),
        LifeSentence("get_up_floor", AbilityDomain.GetUp, Measures.chairStand.id, 16.0),

        // Go: the two-minute step, in stairs and distance.
        LifeSentence("go_one_flight", AbilityDomain.Go, Measures.twoMinuteStep.id, 60.0),
        LifeSentence("go_no_stop", AbilityDomain.Go, Measures.twoMinuteStep.id, 85.0),
        LifeSentence("go_two_flights", AbilityDomain.Go, Measures.twoMinuteStep.id, 110.0),

        // Carry: the push-up, in what the arms will hold.
        LifeSentence("carry_bags", AbilityDomain.Carry, Measures.wallPushUps.id, 8.0),
        LifeSentence("carry_one_trip", AbilityDomain.Carry, Measures.wallPushUps.id, 15.0),
        LifeSentence("carry_overhead", AbilityDomain.Carry, Measures.wallPushUps.id, 25.0),

        // Steady: the one-foot stand, in the moments that need it.
        LifeSentence("steady_sock", AbilityDomain.Steady, Measures.singleLegStance.id, 10.0),
        LifeSentence("steady_trousers", AbilityDomain.Steady, Measures.singleLegStance.id, 20.0),
        LifeSentence("steady_kerb", AbilityDomain.Steady, Measures.singleLegStance.id, 30.0),

        // The other ways of getting around, with their own measures.
        LifeSentence("transfer_bed", AbilityDomain.GetUp, Measures.seatedReach.id, 5.0),
        LifeSentence("transfer_alone", AbilityDomain.GetUp, Measures.seatedReach.id, 10.0),
        LifeSentence("wheel_block", AbilityDomain.Go, Measures.wheelingMinutes.id, 40.0),
        LifeSentence("wheel_shops", AbilityDomain.Go, Measures.wheelingMinutes.id, 80.0),
        LifeSentence("carry_lap", AbilityDomain.Carry, Measures.bandRows.id, 10.0),
        LifeSentence("carry_shelf", AbilityDomain.Carry, Measures.bandRows.id, 20.0),
        LifeSentence("seated_reach_far", AbilityDomain.Steady, Measures.seatedBalance.id, 30.0),

        LifeSentence("bed_sit_up", AbilityDomain.GetUp, Measures.sitToEdge.id, 3.0),
        LifeSentence("bed_edge_alone", AbilityDomain.GetUp, Measures.sitToEdge.id, 6.0),
        LifeSentence("bed_breaths", AbilityDomain.Go, Measures.breathHold.id, 10.0),
        LifeSentence("bed_grip", AbilityDomain.Carry, Measures.gripHold.id, 20.0),
        LifeSentence("bed_ankles", AbilityDomain.Steady, Measures.ankleRange.id, 30.0),
    )

    /**
     * The sentence for one ability, or null when nothing has been measured.
     *
     * The highest threshold the person has actually reached, so the sentence only
     * ever describes something they have done.
     */
    fun forDomain(domain: AbilityDomain, latest: Map<String, Double>): LifeSentence? =
        all.filter { it.domain == domain }
            .filter { sentence -> latest[sentence.measureId]?.let { it >= sentence.atLeast } == true }
            .maxByOrNull { it.atLeast }
}
