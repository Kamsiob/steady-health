package com.kamsiob.steadyhealth.share

/**
 * The four kinds of card. ADDENDUM-03 Part 11.
 *
 * The kind decides which line is suggested and nothing else. Every card is the same
 * size and the same design, and every one of them is editable before it is sent, so
 * the kind is a starting point rather than a template the person is stuck inside.
 */
enum class CardKind(val id: String) {
    /** From the Sunday review. */
    Week("week"),

    /** A tracked item crossing its threshold. Part 11: the one people will send. */
    Milestone("milestone"),

    /** The month, then and now. This one also exports as a PDF. */
    Month("month"),

    /** Their own line, with nothing suggested. */
    Blank("blank"),
    ;

    /** Only the month card is offered as a document as well as a picture. */
    val alsoAPdf: Boolean get() = this == Month

    companion object {
        fun fromId(id: String): CardKind? = entries.firstOrNull { it.id == id }
    }
}

/**
 * What is going on the card, after the person has had it.
 *
 * One line. Not a paragraph, not a heading and a body, not a set of numbers. A card
 * that carries more than one sentence is a report, and Part 11 is explicit that this
 * is a thing somebody sends to one other person rather than a document.
 */
data class CardCopy(val kind: CardKind, val line: String)

/**
 * What may never go on a card, checked rather than trusted.
 *
 * ADDENDUM-03 Part 11: "NEVER ON A CARD: weight, any clinically named measure, any
 * rating out of ten, anything about a condition, anything they did not choose."
 *
 * Every one of those is a thing a person might type themselves, and this is the only
 * feature in the app where something leaves the phone by the person's own hand. So it
 * is checked at the point of rendering, and the check is a warning to the person
 * rather than a refusal: it is their card and their sentence, and an app that silently
 * refused to render somebody's own words would be worse than one that says what it
 * noticed and lets them decide.
 *
 * The suggested lines are held to the same rule by a test, so nothing the app itself
 * proposes ever trips it.
 */
object CardRules {

    /** The longest a line may be before it stops fitting the design. */
    const val MOST_CHARACTERS = 120

    /**
     * Words that mean weight, a clinical measure, a rating, or a condition.
     *
     * Deliberately short. This is a nudge at the moment of sending and not a filter on
     * somebody's vocabulary, and a long list would fire on ordinary sentences until
     * people stopped reading it.
     */
    private val weight = listOf("kg", "kilo", "kilos", "lb", "lbs", "pound", "pounds", "stone")

    private val clinical = listOf(
        "bmi", "blood pressure", "cholesterol", "diagnosis", "diagnosed",
        "condition", "arthritis", "osteoporosis", "prescription", "medication",
    )

    private val outOfTen = Regex("""\b(\d|10)\s*(/|out of)\s*10\b""", RegexOption.IGNORE_CASE)

    /** Everything the line contains that Part 11 says never goes on a card. */
    fun worthAWord(line: String): List<String> = buildList {
        addAll(weight.filter { says(line, it) })
        addAll(clinical.filter { says(line, it) })
        if (outOfTen.containsMatchIn(line)) add("a rating out of ten")
    }

    fun tooLong(line: String): Boolean = line.trim().length > MOST_CHARACTERS

    private fun says(text: String, word: String): Boolean =
        Regex("(?<![\\p{L}\\p{N}])${Regex.escape(word)}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)
}
