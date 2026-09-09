package com.kamsiob.steadyhealth.places

import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R

/**
 * What somebody said about one of the six places.
 *
 * Three answers rather than two. "Not sure" is here because four of the six
 * questions are about a room somebody is not standing in while they answer, and a
 * walkthrough that forces a yes or a no gets guesses instead of answers. It is
 * treated as an answer and not as a skip: the question is done, and it can be
 * looked at whenever.
 */
enum class Said(val id: String) {
    /** Already the way the question describes. Nothing to offer. */
    Sorted("sorted"),

    /** Not that way yet. This is the answer that has a fix under it. */
    NotYet("not_yet"),

    /** Worth a look later. Also offers the fix, because it costs nothing to read. */
    NotSure("not_sure"),
    ;

    /** Whether the plain fix is worth showing under this answer. */
    val wantsTheFix: Boolean get() = this != Sorted

    companion object {
        fun fromId(id: String): Said? = entries.firstOrNull { it.id == id }
    }
}

/**
 * One of the six, from ADDENDUM-03 Part 8 item 2.
 *
 * The words are resources rather than literals, so that they are translated with
 * everything else and so that the banned word check, which reads every string
 * resource, reads these too.
 */
data class PlaceQuestion(
    val id: String,
    /** The room or the moment, two or three words. */
    @param:StringRes val where: Int,
    /** The question itself, asked about how things are rather than about danger. */
    @param:StringRes val question: Int,
    /** One plain thing to do about it. Never a product and never a warning. */
    @param:StringRes val fix: Int,
)

/**
 * The places you move through. ADDENDUM-03 Part 8 item 2.
 *
 * Six questions, once, repeatable. The spec is unusually specific about the framing
 * and the reason is worth writing down: every app that has ever asked these questions
 * has called them a home safety assessment, given them a score out of ten, and sold
 * the person a grab rail at the end. That version is why most people never finish it.
 *
 * So: no score, no total, no products, no named brand, and nothing in the copy about
 * falling, risk or danger. The frame is the places you move through, which is what
 * they are. Somebody who answers all six and changes nothing has still done the
 * walkthrough, and the app says nothing about it afterwards.
 *
 * "Each answer offers one plain fix" is the whole payload, so each fix is one sentence
 * somebody could act on this afternoon with what they already own. Where the honest
 * fix does cost money, the sentence says the cheap version.
 *
 * Repeatable rather than one time. Houses change, and somebody who has just come home
 * from an operation is walking a different route through the same rooms.
 */
object Places {

    val all: List<PlaceQuestion> = listOf(
        PlaceQuestion(
            id = "stairs_light",
            where = R.string.place_stairs_light_where,
            question = R.string.place_stairs_light_question,
            fix = R.string.place_stairs_light_fix,
        ),
        PlaceQuestion(
            id = "night_route",
            where = R.string.place_night_route_where,
            question = R.string.place_night_route_question,
            fix = R.string.place_night_route_fix,
        ),
        PlaceQuestion(
            id = "rugs_cables",
            where = R.string.place_rugs_cables_where,
            question = R.string.place_rugs_cables_question,
            fix = R.string.place_rugs_cables_fix,
        ),
        PlaceQuestion(
            id = "something_to_hold",
            where = R.string.place_something_to_hold_where,
            question = R.string.place_something_to_hold_question,
            fix = R.string.place_something_to_hold_fix,
        ),
        PlaceQuestion(
            id = "near_the_bed",
            where = R.string.place_near_the_bed_where,
            question = R.string.place_near_the_bed_question,
            fix = R.string.place_near_the_bed_fix,
        ),
        PlaceQuestion(
            id = "indoor_shoes",
            where = R.string.place_indoor_shoes_where,
            question = R.string.place_indoor_shoes_question,
            fix = R.string.place_indoor_shoes_fix,
        ),
    )

    fun byId(id: String): PlaceQuestion? = all.firstOrNull { it.id == id }

    /** How many there are, for the screen that says one of six. */
    val count: Int get() = all.size

    /**
     * Whether the walkthrough has been finished, from what was answered.
     *
     * Every question answered, in any way. There is no partial state worth naming and
     * nothing anywhere counts how many were sorted, because counting them is the
     * score this feature exists without.
     */
    fun finished(answers: Map<String, Said>): Boolean =
        all.all { it.id in answers }

    /**
     * The ones with something to do about them, in the order they were asked.
     *
     * This is a list to read, not a list of jobs. Nothing in the app ever asks
     * whether one of them was done, and nothing counts them.
     */
    fun worthALook(answers: Map<String, Said>): List<PlaceQuestion> =
        all.filter { answers[it.id]?.wantsTheFix == true }
}
