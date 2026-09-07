package com.kamsiob.steadyhealth.ai

/**
 * The words the app will not say, and the checks that keep them out.
 *
 * This is the one place the banned list lives in shipping code. The voice test
 * keeps its own copy on purpose, so that a mistake here has to be made twice
 * before it reaches anybody.
 */
object Words {

    /** DESIGN.md section 6. Every one of these is banned everywhere. */
    val banned: List<String> = listOf(
        "rung", "tier", "trail", "trend", "postcard", "story", "check-in",
        "streak", "score", "goal", "target", "calories", "calorie", "burn",
        "earn", "cheat", "fail", "should", "must",
        "senior", "elderly", "frail", "frailty", "fall risk", "decline",
        "sarcopenia", "patient", "diagnosis", "prescribe",
        "report", "assessment", "evaluation", "findings", "results",
        "recommendations",
    )

    /**
     * The clinical vocabulary the visit summary adds. AI.md job 6, check 6.
     *
     * Wider than the banned list because a summary a clinician reads is the one
     * place where a model reaching for a medical word does real harm.
     */
    val clinical: List<String> = listOf(
        "diagnos",
        "condition",
        "disease",
        "syndrome",
        "arthritis",
        "sarcopenia",
        "frailty",
        "deficiency",
        "deficit",
        "weakness",
        "risk",
        "symptom",
        "treat",
        "prescribe",
        "dose",
        "medication",
        "therapy",
    )

    /**
     * The phrases that mean the model has reached past the data. AI.md job 6,
     * check 7.
     *
     * Every one of them joins two facts into a claim about why, and why is not in
     * the brief and never will be.
     */
    val causal: List<String> = listOf(
        "because",
        "due to",
        "caused by",
        "which means",
        "suggests",
        "indicates",
    )

    /** The words that carry a quantity without being a numeral. */
    val quantities: List<String> = listOf(
        "twice",
        "three times",
        "half",
        "most",
        "all",
        "none",
        "double",
        "every",
    )

    /** True when [text] contains [word] as a whole word, case-insensitively. */
    fun says(text: String, word: String): Boolean =
        Regex("(?<![\\p{L}\\p{N}])${Regex.escape(word)}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)

    /** True when [text] contains [stem] as the start of a word ("diagnos"). */
    fun saysStem(text: String, stem: String): Boolean =
        Regex("(?<![\\p{L}\\p{N}])${Regex.escape(stem)}", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)

    fun bannedWordsIn(text: String): List<String> = banned.filter { says(text, it) }

    fun clinicalWordsIn(text: String): List<String> = clinical.filter { saysStem(text, it) }

    fun causalPhrasesIn(text: String): List<String> = causal.filter { says(text, it) }

    /** Split into sentences, keeping it simple because the inputs are short. */
    fun sentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
}

/**
 * The job 3 post-filter, from AI.md and LOGIC.md section 10.
 *
 * The rule: no sentence may put a restriction tag and a weight direction
 * together. It exists because that sentence is the app inventing a cause, and
 * because the cause it would invent ("you ate light and your weight went down")
 * is the one this app was built not to say.
 *
 * It is a filter and not a prompt instruction, because a prompt instruction is a
 * request and this is a guarantee.
 */
object WeekFilter {

    /** The words that mean eating less, in the sentences a model might write. */
    private val restrictionWords = listOf(
        "ate light", "ate less", "skipped", "fasting", "fasted", "light meal",
        "small meals", "cut back", "snacked", "late night",
    )

    private val weightWords = listOf(
        "weight",
        "a little lower",
        "a little higher",
        "lighter",
        "heavier",
    )

    fun offends(sentence: String): Boolean =
        restrictionWords.any { Words.says(sentence, it) } &&
            weightWords.any { Words.says(sentence, it) }

    /** The paragraph with any offending sentence taken out. */
    fun clean(paragraph: String): String =
        Words.sentences(paragraph).filterNot(::offends).joinToString(" ")

    fun clean(note: WeekNote): WeekNote =
        note.copy(paragraphs = note.paragraphs.map(::clean).filter { it.isNotBlank() })
}
