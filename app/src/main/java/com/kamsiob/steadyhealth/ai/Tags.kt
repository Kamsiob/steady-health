package com.kamsiob.steadyhealth.ai

/** What a tag is about. Only used to group the grid; nothing depends on it. */
enum class TagGroup(val id: String) {
    Food("food"),
    Sleep("sleep"),
    Mood("mood"),
    Movement("movement"),
    Body("body"),
    Life("life"),
}

/**
 * One of the twenty-four.
 *
 * [id] is what is stored and what the model may return. It never changes. [group]
 * is only for laying out the grid.
 */
data class Tag(val id: String, val group: TagGroup)

/**
 * The twenty-four tags, fixed, from AI.md job 2.
 *
 * Fixed is the whole design. The model picks from this list and can return
 * nothing else, which is what makes an unconstrained language model safe to point
 * at somebody's diary. A larger vocabulary would be a better classifier and a
 * worse promise.
 *
 * No tag is a judgement. "Overate", "binged", "cheated" and "bad day" do not
 * exist here and cannot be produced, because a word the app does not have is a
 * word it cannot say back to somebody at the end of a hard week.
 *
 * Adding one is a deliberate act by the person, in settings, and the app asks
 * first whether an existing tag fits.
 */
object Tags {

    val all: List<Tag> = listOf(
        Tag("ate_out", TagGroup.Food),
        Tag("cooked", TagGroup.Food),
        Tag("ate_light", TagGroup.Food),
        Tag("ate_a_lot", TagGroup.Food),
        Tag("late_night", TagGroup.Food),
        Tag("snacked", TagGroup.Food),

        Tag("slept_well", TagGroup.Sleep),
        Tag("slept_badly", TagGroup.Sleep),
        Tag("short_sleep", TagGroup.Sleep),
        Tag("rested", TagGroup.Sleep),

        Tag("stressed", TagGroup.Mood),
        Tag("calm", TagGroup.Mood),
        Tag("low", TagGroup.Mood),
        Tag("good", TagGroup.Mood),

        Tag("walked", TagGroup.Movement),
        Tag("active", TagGroup.Movement),
        Tag("sat_a_lot", TagGroup.Movement),

        Tag("sore", TagGroup.Body),
        Tag("pain", TagGroup.Body),
        Tag("unwell", TagGroup.Body),

        Tag("busy", TagGroup.Life),
        Tag("travel", TagGroup.Life),
        Tag("social", TagGroup.Life),
        Tag("family", TagGroup.Life),
    )

    /** How many there are, held by a test, because the number is the promise. */
    const val COUNT = 24

    val ids: Set<String> = all.map { it.id }.toSet()

    fun byId(id: String): Tag? = all.firstOrNull { it.id == id }

    fun inGroup(group: TagGroup): List<Tag> = all.filter { it.group == group }

    /**
     * The tags that describe eating less, or later, or differently.
     *
     * They exist because people write about food and the app should hear it. They
     * are kept in a named set because two rules turn on it: LOGIC.md never lets
     * one of them appear in the same sentence as a weight direction, and AI.md
     * never passes one to the pattern job at all. Between them those rules stop
     * the app from ever implying that eating less made a number move.
     */
    val restriction: Set<String> = setOf("ate_light", "late_night", "snacked")

    /** At most this many come back from one sentence. AI.md job 2. */
    const val MOST_PER_SENTENCE = 3
}
