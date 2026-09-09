package com.kamsiob.steadyhealth.ai

import kotlin.math.abs

/**
 * Why a piece of a reading was rejected.
 *
 * One per check in AI.md job 9, and then one per line of ADDENDUM-03 Part 7 that no
 * word on AI.md's banned list gives away.
 */
enum class ReadingFault {
    /** A capitalised name that stands nowhere in the extracted text. */
    InventedName,

    /** A number, in digits or in words, that the document does not carry. */
    InventedNumber,

    /** A month, season or weekday the document never mentions. */
    InventedDate,

    /** A unit the document does not use, which is how a measurement gets made up. */
    InventedMeasurement,

    /** A condition the document does not name, which is the app diagnosing. */
    InventedCondition,

    /** A term explained that the document never used. */
    UnsourcedTerm,

    /** Quotation marks around words the document does not carry. */
    FabricatedQuote,

    /** A word from job 9's banned list, which is held in this file. */
    ForbiddenWord,

    /** "because", "which means", and the rest: the reading reaching past the page. */
    CausalClaim,

    /** A verdict on the news, however gently it is worded. */
    Judgement,

    /** Telling the person what to do, which Part 7 forbids in any wording. */
    Advice,

    /** A number read against people in general rather than against the page. */
    PopulationClaim,

    /** Questioning or contradicting the clinician who wrote the document. */
    SecondGuess,

    /** Food, sleep, mood or medicine: outside movement, therapy and care. */
    OutOfScope,

    /** A question that shares nothing with the document. */
    UngroundedQuestion,

    /** A question written as a finding, with no question mark on the end. */
    NotAQuestion,
}

/**
 * One paragraph, one term or one question, with everything that was wrong with it.
 *
 * Job 9 fails in pieces. AI.md says regenerate once, then drop what failed, and a
 * reading is still worth showing with one of its paragraphs gone, so the verdict is
 * per piece and the caller drops only what did not pass. [piece] comes back rather
 * than an index, so that a caller assembling a screen never has to line two lists up
 * by position.
 */
data class PieceVerdict<T>(
    val piece: T,
    val faults: List<ReadingFault>,
    val detail: List<String>,
) {
    val passed: Boolean get() = faults.isEmpty()
}

/**
 * What survived a reading, and what did not.
 *
 * [paragraphsAllFailed] has its own name because it has its own screen copy: AI.md
 * job 9 says that when the plain words section fails entirely the app shows the
 * terms and the photo and says "The app could only pull out the words this time."
 * Unlike job 6's allFailed it is also true when the model returned no paragraphs at
 * all, because the question the screen is asking is whether there is a plain words
 * section to show, and an empty one and a rejected one look the same to the person
 * holding the phone.
 */
data class ReadingVerdict(
    val kind: PieceVerdict<String>,
    val paragraphs: List<PieceVerdict<String>>,
    val terms: List<PieceVerdict<ReadingTerm>>,
    val questions: List<PieceVerdict<String>>,
    val trimmedParagraphs: Int,
) {
    /** Blank when the kind failed, which is the shape Reading.kt asks for. */
    val keptKind: String get() = if (kind.passed) kind.piece else ""

    val keptParagraphs: List<String> get() = paragraphs.filter { it.passed }.map { it.piece }

    val keptTerms: List<ReadingTerm> get() = terms.filter { it.passed }.map { it.piece }

    val keptQuestions: List<String> get() = questions.filter { it.passed }.map { it.piece }

    /** True when there is no plain words section left to show. */
    val paragraphsAllFailed: Boolean get() = keptParagraphs.isEmpty()

    /** The reading with only what survived in it, ready to hand to a screen. */
    val kept: Reading get() = Reading(keptKind, keptParagraphs, keptTerms, keptQuestions)

    val faults: List<ReadingFault>
        get() = buildList {
            addAll(kind.faults)
            paragraphs.forEach { addAll(it.faults) }
            terms.forEach { addAll(it.faults) }
            questions.forEach { addAll(it.faults) }
        }.distinct()
}

/**
 * The job 9 validator, from AI.md and ADDENDUM-03 Part 7.
 *
 * Built before MedGemma is wired in, for the same reason job 6's was: a reading is
 * the app's own words laid over somebody's medical letter, and the sentence saying
 * what that letter means for them is the one thing this feature exists not to write.
 * A prompt asking for that is a request. This is a check.
 *
 * Deterministic and offline. No model, no randomness, no clock, so its verdict on an
 * output is the same today and in five years.
 *
 * There is one source for everything: the extracted text. Job 9 has no brief and no
 * facts, only the document, so every rule here is a rule about tracing a word back
 * to it, and a word that cannot be traced is dropped rather than rewritten.
 *
 * Tracing is half of it, and the smaller half. It catches what a reading made up. It
 * says nothing about what the reading did with what it found, and most of Part 7 is
 * about exactly that: a verdict, a piece of advice, a comparison with people in
 * general are built either out of words that are on the page already or out of no
 * particular words at all. Those are caught by name, in the lists below.
 *
 * Four costs are known and kept, because each check behind them catches something
 * real and no narrower version of it does. A sentence is rejected for "because"
 * even where the because sits inside a quotation of the page, since AI.md rejects
 * the sentence rather than the voice. "Due to" goes with it, so a reading writes
 * that the review is due on 9 April and never that it is due to happen then. A
 * reading may not say the note gave advice, because "advice" is also how a model
 * introduces its own. And "grade" is held as a unit, which costs a plain line
 * saying the note graded the muscle, because "grade 4 out of 5" on a page that
 * says 4/5 is a scale invented around the page's own numbers.
 */
object ReadingValidator {

    /** How long a word has to be before a question can be grounded on it. */
    const val SHORTEST_CONTENT_WORD = 5

    /** How short an abbreviation may be and still count. "ROM" is the point. */
    const val SHORTEST_ABBREVIATION = 2

    /** How close two numbers have to be to count as the same one. Job 6's figure. */
    private const val CLOSE_ENOUGH = 0.051

    /**
     * Job 9's own banned list, quoted from AI.md, and kept apart from [Words.banned]
     * on purpose.
     *
     * The two lists disagree and they have to. DESIGN.md section 6 bans "report",
     * "assessment", "findings" and "results" across the app, while job 9's first line
     * is what kind of document this is, which is very often the words "progress
     * report", and ADDENDUM-03's own worked example explains "manual muscle testing".
     * Running the app-wide voice list over a reading would reject nearly every honest
     * one. Going the other way is worse: "better" and "worse" are how the rest of the
     * app names an ability's direction, and "normal" is an ordinary word everywhere
     * else, so folding job 9's list into [Words.banned] would break copy that is
     * required elsewhere. So this list lives in the one file that uses it.
     *
     * These entries fire as whole words, which is why "should" does not fire inside
     * "shoulder", a word a reading of a physio letter will print constantly.
     */
    val bannedWords: List<String> = listOf(
        "should",
        "worse",
        "better",
        "means that",
        "good news",
        "bad news",
        "healthy",
        "unhealthy",
    )

    /**
     * The same list where an entry has to catch a word's endings: "recommend" has to
     * fire on "recommends" and "recommended" as well.
     *
     * Each stem is a listed word cut back and no further. "worr" is worry and nothing
     * else in English. "healthi" is healthier and healthiest, and the bare word
     * "health" is deliberately absent, because a reading of an insurance letter may
     * honestly say "your health plan". The cost is admitted rather than hidden:
     * "condition" also fires on "conditioning", a movement word the app uses
     * elsewhere and a reading has no reason to.
     */
    val bannedStems: List<String> = listOf(
        "diagnos", "prognos", "condition", "disease", "recommend", "suggest",
        "indicat", "improv", "declin", "worsen", "worr", "concern",
        "normal", "abnormal", "healthi", "unhealthi",
    )

    /**
     * The five phrases AI.md rejects a sentence for.
     *
     * [Words.causal] is job 6's list and carries "indicates" as well, which job 9
     * bans as a word anyway, so it is caught either way. They stay separate so that
     * an edit to job 6's list cannot quietly change what a reading may say.
     *
     * Matched literally, never loosely. ADDENDUM-03's own example plain line says
     * "which usually means", and widening "which means" into a pattern would reject
     * the sentence the addendum wrote to show what good looks like.
     */
    val causal: List<String> = listOf(
        "because",
        "due to",
        "caused by",
        "which means",
        "suggests",
    )

    /**
     * The same move as [causal], in the words AI.md's five do not cover.
     *
     * The five above are the specification and are quoted exactly, so they are left
     * exactly as they are. These are what the specification was plainly for: a model
     * told not to write "because" writes "thanks to" or "as a result", and the
     * sentence still says why one thing followed from another.
     *
     * Matched from the start of a word, so "reflect" covers "reflects" and
     * "reflecting". "the reason" is deliberately absent, because "what is the reason
     * for the sling" is the question section doing its job. "comes from" is absent
     * for the same kind of reason: a note comes from a clinic.
     */
    val alsoCausal: List<String> = listOf(
        "thanks to", "as a result", "which is why", "that is why", "led to",
        "leads to", "leading to", "resulted in", "results in", "reflect",
        "explains why", "put down to", "owing to", "on account of",
    )

    /**
     * Saying whether the news is good or bad, in the words left over once "better",
     * "improving" and "worrying" have gone.
     *
     * Three of Part 7's lines land here and they are one check, because they are one
     * sentence with the sign flipped: never says whether the news is good or bad,
     * never characterises progress, never reassures, never says whether to be worried
     * and never says not to be. "Nothing here is out of the ordinary" is as much a
     * verdict as "this is good news", and it carries no banned word, no number, no
     * name and no condition, so nothing else in this file sees it.
     *
     * The comparisons are here for the same reason as the euphemisms. "Your flexion
     * is further than it was at the first visit" names a direction out of two numbers
     * the page carries, so the number check has nothing to say about it, and naming a
     * direction is the whole of characterising progress.
     *
     * Absent on purpose, each for a reason worth keeping: "positive" and "negative"
     * are how a note records a special test, and a reading restating the note's own
     * result is quoting the page. "step up" and "step forward" are movements, and a
     * home programme lists them. "alarm" is a thing an older person wears, so the
     * entries here are "alarmed" and "alarming". "confidence" is what a physiotherapy
     * note calls walking without holding on, so only "confident" is listed.
     */
    val judgements: List<String> = listOf(
        "right direction", "wrong direction", "the right way", "the wrong way",
        "on track", "off track", "big step", "come along", "coming along",
        "well on the way", "within reach", "out of the ordinary", "good sign",
        "bad sign", "well done", "looks good", "looking good", "going well",
        "went well", "doing well", "doing everything", "you would want",
        "where it needs to be", "encouraging", "promising", "impressive",
        "pleasing", "pleased", "disappointing", "setback", "unfortunately",
        "fortunately", "thankfully", "reassur", "alarmed", "alarming",
        "anxious", "afraid", "confident", "progressing", "progressed",
        "getting there", "stronger", "weaker", "quicker", "faster", "slower",
        "steadier", "easier", "harder", "mild", "severe", "significant",
        "than it was", "than they were", "than you were", "than before",
        "than at the",
    )

    /**
     * Telling the person what to do, in the words left over once "should",
     * "recommend" and "suggest" have gone.
     *
     * Part 7: never advises, not on exercise, treatment, medication, or whether to
     * follow a plan. Half the list is imperatives and half is the politeness a model
     * wraps them in, because "it might be worth mentioning this at the next visit" is
     * an instruction with a hedge in front of it.
     *
     * It fires inside a question as well as inside a paragraph, and that is the
     * point rather than a side effect. "Could you ask whether to keep going with the
     * overhead work" is the app saying keep going, with a question mark on the end.
     *
     * "continue" is absent, because a note says continue and a reading is allowed to
     * say that the note said it.
     */
    val advice: List<String> = listOf(
        "keep doing", "keep up", "keep going", "carry on", "stick to",
        "stick with", "stay with", "work up to", "build up to", "ease off",
        "cut back", "make sure", "be sure", "try to", "try and", "you need to",
        "no need to", "do not need to", "worth asking", "worth doing",
        "worth mentioning", "worth checking", "will help", "would help",
        "advis", "advice", "ought to", "best to", "aim for", "aim to",
        "focus on", "remember to", "be careful", "sensible", "good idea",
        "reasonable to", "no need for", "must",
    )

    /**
     * Reading a number against people in general.
     *
     * Part 7 draws this line on its own, and AI.md defends it with two words,
     * "normal" and "abnormal". Neither is in "your flexion is in the usual range for
     * someone your age", which is the same claim and the one a model makes once it
     * has been told not to say normal.
     *
     * "typical" is here and "usually" is not. ADDENDUM-03's own worked plain line is
     * "which usually means it moves against resistance", and a check that rejects the
     * example the addendum wrote to show what good looks like is the wrong check. The
     * cost is that "typical" also fires on "typically", which a plain line can do
     * without.
     */
    val population: List<String> = listOf(
        "most people", "other people", "few people", "many people",
        "people your age", "for your age", "routine", "unexpected",
        "someone your age", "your age group", "average", "typical",
        "usual range", "usual for", "usually does", "the norm", "than most",
        "than others", "ahead of where", "behind where", "expected range",
        "expected for", "what is expected", "population", "peers",
        "compared with others", "compared to others",
    )

    /**
     * Second guessing the clinician.
     *
     * Part 7: never contradicts, questions or second guesses what a clinician wrote.
     * Every sentence on this line is hedged, because a model does not contradict a
     * physiotherapist outright. It says the number "seems low", or that the clinician
     * "might not have" seen the earlier one. The hedges are the list.
     */
    val secondGuesses: List<String> = listOf(
        "second opinion", "another opinion", "may not have", "might not have",
        "may have missed", "might have missed", "seems low", "seems high",
        "looks low", "looks high", "does not seem", "hard to see why",
        "disagree", "in fact", "even though the note", "although the note",
        "unnecessary", "out of date",
    )

    /**
     * Food, sleep, mood and medicine.
     *
     * Part 7's last line: never handles anything outside movement, therapy or care
     * logistics. Medication is named twice in Part 7, once as advice and once as
     * scope, and this is the scope reading, so the word is out of a reading whatever
     * it is doing there.
     *
     * Care logistics is wide on purpose, and an insurance letter, a referral, a
     * clinic, an appointment and a dietitian's name are all inside it. That is why
     * "diet" is absent and "eat" is not: the referral is logistics, the advice about
     * lunch is not. "depression" is absent because a physiotherapy note means the
     * shoulder girdle by it.
     */
    val outOfScope: List<String> = listOf(
        "medicat", "medicin", "tablet", "dose", "drug", "painkiller", "eat",
        "food", "meal", "protein", "nutrition", "calorie", "sleep", "mood",
        "anxiety", "alcohol", "smok", "cigarette",
    )

    /**
     * The whole reading, checked against the text it came from.
     *
     * The ceiling on paragraphs is enforced here and the floor is not, which is
     * Reading.MOST_PARAGRAPHS' decision and its reasoning: a sixth paragraph is past
     * what anybody reads on a phone, so it is trimmed and counted, while a reading
     * that came back short is short and not wrong.
     */
    fun check(reading: Reading, text: String): ReadingVerdict {
        val paragraphs = reading.paragraphs.take(Reading.MOST_PARAGRAPHS)
        return ReadingVerdict(
            kind = checkKind(reading.kind, text),
            paragraphs = paragraphs.map { checkParagraph(it, text) },
            terms = reading.terms.map { checkTerm(it, text) },
            questions = reading.questions.map { checkQuestion(it, text) },
            trimmedParagraphs = reading.paragraphs.size - paragraphs.size,
        )
    }

    /**
     * What kind of document it is, which AI.md says comes from the document itself.
     *
     * A blank kind passes, because a blank kind is one that has already been dropped
     * and Reading.kt says so.
     */
    fun checkKind(kind: String, text: String): PieceVerdict<String> =
        verdict(kind, appWords(kind, text))

    /** One plain words paragraph. Every sentence in it traces back to the document. */
    fun checkParagraph(paragraph: String, text: String): PieceVerdict<String> =
        verdict(paragraph, appWords(paragraph, text))

    /**
     * One term and its plain line, and the one place a document's own clinical word
     * may be printed at all.
     *
     * Reading.kt says why: [ReadingTerm.term] is the document's word, quoted under a
     * heading that says whose it is, so the banned list does not fire on it while it
     * stands verbatim in the extracted text. A term that is not in the text is not a
     * quotation, it is the app naming something, and it is then checked like anything
     * else the app wrote.
     *
     * [ReadingTerm.plain] is checked in full, including a banned word that does
     * appear in the document, and that is the deliberate part. The plain line is the
     * app speaking in its own sentence. "This means your condition is stable" is the
     * app saying "condition" however the letter was worded, and a word carried across
     * from the page into the app's own line is the app adopting it. The term field is
     * where that word already has its place, so the plain line has no need of it.
     * Quotation marks are the one exception, and [forbidden] says why.
     *
     * The plain line is also the one place where a capital letter is not a claim to
     * a name: "TUG" is answered by "Timed Up and Go", and [expansionOf] lets a line
     * spell out its own abbreviation without the proper noun check taking two
     * ordinary words for a clinic.
     */
    fun checkTerm(term: ReadingTerm, text: String): PieceVerdict<ReadingTerm> {
        val quoted = Words.says(text, term.term)
        val found = buildList {
            if (!quoted) {
                add(ReadingFault.UnsourcedTerm to "\"${term.term}\" is not in the document")
                addAll(forbidden(term.term, text))
            }
            addAll(appWords(term.plain, text, expansionOf(term.term, term.plain)))
        }
        return verdict(term, found)
    }

    /**
     * One question, which AI.md asks two things of.
     *
     * It has to reference something in the document, which is read here as sharing a
     * content word with it, and it has to be phrased as a question rather than as a
     * finding, which is read as ending in a question mark. Both are checks because
     * "Your flexion has improved" with the mark taken off is a finding, and a
     * question about nothing in the letter is inference wearing a question mark.
     */
    fun checkQuestion(question: String, text: String): PieceVerdict<String> {
        val found = buildList {
            if (!question.trim().endsWith("?")) {
                add(ReadingFault.NotAQuestion to "\"$question\" is not asked as a question")
            }
            if (contentWordsIn(question).none { Words.says(text, it) }) {
                add(ReadingFault.UngroundedQuestion to "\"$question\" shares nothing with the page")
            }
            addAll(appWords(question, text))
        }
        return verdict(question, found)
    }

    /** Job 9's banned list, whole words and stems together. */
    fun bannedWordsIn(text: String): List<String> =
        bannedWords.filter { Words.says(text, it) } +
            bannedStems.filter { Words.saysStem(text, it) }

    /**
     * The phrases that turn two things the document said into one claim about why.
     *
     * Three sources, in the order they were added: AI.md's five, the words that do
     * the same work, and the one piece of punctuation that does. A comma and "so" is
     * a causal join in the plainest English there is, and it is matched only in that
     * shape, because "so that the shoulder settles" is a purpose a note can state.
     */
    fun causalPhrasesIn(text: String): List<String> =
        causal.filter { Words.says(text, it) } +
            alsoCausal.filter { Words.saysStem(text, it) } +
            if (SO_JOIN.containsMatchIn(text)) listOf(", so") else emptyList()

    /**
     * True when a quoted span really stands in the extracted text.
     *
     * Compared with the punctuation taken out and the case flattened, because a
     * model quoting a note writes "continue the scapular setting" for a page that
     * starts that sentence with a capital, and puts the full stop inside the closing
     * mark about as often as outside it. Matching character for character would fail
     * honest quotations while teaching a dishonest one nothing: the words are what a
     * quotation claims, and the punctuation around them is not.
     */
    private fun quotesTheDocument(quote: String, text: String): Boolean =
        flattened(text).contains(flattened(quote))

    /**
     * The words of a plain line that spell out an abbreviation, when the line opens
     * by spelling it out.
     *
     * "TUG" is answered by "Timed Up and Go", and two of those three words are
     * capitalised without being anybody's name. [properNounsIn] cannot tell them
     * from a clinic it has never heard of, so without this it drops the one kind of
     * plain line the terms section exists for, the one where the expansion is longer
     * than the abbreviation and is the whole answer.
     *
     * Only the head of the line is read, up to the first comma or full stop, and
     * only in order: a word whose first letter is the next letter of the term is
     * taken, a small joining word is stepped over, anything else stops the walk. If
     * the letters do not run out, nothing is exempted, which is the safe way to be
     * wrong.
     */
    private fun expansionOf(term: String, plain: String): List<String> {
        if (!isAbbreviation(term)) return emptyList()
        val letters = term.filter { it.isLetter() }.uppercase()
        val taken = mutableListOf<String>()
        var next = 0
        for (match in WORD.findAll(plain.takeWhile { it != ',' && it != '.' })) {
            val word = match.value
            val spells = next < letters.length && word.first().uppercaseChar() == letters[next]
            when {
                spells -> {
                    taken.add(word)
                    next++
                }
                word.lowercase() in JOINING_WORDS -> Unit
                else -> break
            }
        }
        val spelledOut = next == letters.length
        return if (spelledOut) taken.filter { it.first().isUpperCase() } else emptyList()
    }

    /**
     * The capitalised words a piece is claiming as names.
     *
     * The heuristic, stated plainly because it is one: a proper noun is a capitalised
     * word that is not the first word of its sentence and is not an ordinary word
     * that happens to be capitalised. That catches a clinic, a surname, a place, a
     * month or an abbreviation the model reached for. It does not catch a name at the
     * start of a sentence, or one standing after an abbreviation's own full stop, as
     * in "Dr. Raghavan", and it cannot tell a name from a heading. The trade is job
     * 6's: a check that is right about what it claims and silent about the rest is
     * worth more than one that guesses, and the number, date and unit checks cover
     * ground this one cannot reach.
     */
    fun properNounsIn(text: String): List<String> =
        WORD.findAll(text)
            .filterNot { opensASentence(text, it.range.first) }
            .map { it.value }
            .filter { it.first().isUpperCase() }
            .filterNot { it in ORDINARY_CAPITALS }
            .distinct()
            .toList()

    /**
     * True when the word starting at [at] is the first word of a sentence.
     *
     * Everything in front of it is whitespace and closing quotation marks back to a
     * full stop, or there is nothing in front of it at all. The quotation marks are
     * the part that earns its place: a model writes the sling line as "Please bring
     * the sling to the next appointment." and starts the next sentence after the
     * closing mark, and reading only the full stop leaves that sentence's first word
     * standing in the middle of one, where an ordinary capitalised word is taken for
     * a name the page never carried.
     */
    private fun opensASentence(text: String, at: Int): Boolean {
        val before = text.take(at).trimEnd { it.isWhitespace() || it in CLOSERS }
        return before.isEmpty() || before.last() in SENTENCE_ENDS
    }

    /**
     * The condition names in a piece of writing, by the shape of the word and by a
     * short list of the ones that have no telltale shape.
     *
     * Narrow on purpose, and it says so rather than pretending otherwise: this
     * catches a model reaching for a diagnosis, not every diagnosis in English. A
     * check that is right about what it claims is worth more than one that guesses,
     * which is the same trade [properNounsIn] makes.
     */
    fun conditionNamesIn(text: String): List<String> =
        CONDITION_NAMES.filter { Words.says(text, it) } +
            WORD.findAll(text)
                .map { it.value }
                .filter { word -> CONDITION_ENDINGS.any { word.endsWith(it, ignoreCase = true) } }
                .filter { bannedWordsIn(it).isEmpty() }
                .toList()

    /**
     * The words a question could be about. Long enough to carry meaning, or an
     * abbreviation, which is short by nature and is exactly what a question about a
     * therapist's letter tends to be about.
     */
    fun contentWordsIn(text: String): List<String> =
        WORD.findAll(text)
            .map { it.value }
            .filter { it.length >= SHORTEST_CONTENT_WORD || isAbbreviation(it) }
            .filterNot { it.lowercase() in QUESTION_STOPWORDS }
            .distinct()
            .toList()

    /**
     * The numbers a piece writes out in words, put back together the way a person
     * writes them rather than counted one word at a time.
     *
     * "One hundred and eighteen" is three number words and one number. Read a word
     * at a time it faults the hundred and the eighteen against a page that plainly
     * says 118, and so does "ninety-six", which is the shape every number over
     * twenty takes when a paragraph spells it out. That is an honest sentence about
     * the document's own measurement thrown away for its grammar.
     *
     * The composing is the least arithmetic that reads English: a word adds its
     * value and "hundred" multiplies what is standing so far. "And" holds a run open
     * only directly after "hundred", so that "one hundred and eighteen" is one
     * number while "three and four" stays two, which is the difference between
     * reading the page and doing sums on it. The values come from job 6's table
     * through [SummaryValidator.spelledWords], because two tables would drift.
     */
    private fun spelledNumbersIn(text: String): List<Pair<String, Double>> =
        spelledRuns(text).map { it.joinToString(" ") to composed(it) }

    /** Everything that is checked the same way wherever the app is the one speaking. */
    private fun appWords(
        piece: String,
        text: String,
        expansion: List<String> = emptyList(),
    ): List<Pair<ReadingFault, String>> =
        names(piece, text, expansion) +
            conditions(piece, text) +
            numbers(piece, text) +
            dates(piece, text) +
            measurements(piece, text) +
            quotations(piece, text) +
            forbidden(piece, text) +
            claims(piece) +
            part7(piece, text)

    private fun names(
        piece: String,
        text: String,
        expansion: List<String>,
    ): List<Pair<ReadingFault, String>> =
        properNounsIn(piece)
            .filterNot { it in expansion }
            .filterNot { Words.says(text, it) }
            .map { ReadingFault.InventedName to "\"$it\" is not in the document" }

    /**
     * A condition the document did not name.
     *
     * AI.md's first "never" for this job is that the app never names a condition the
     * document did not name, and nothing else in this file reaches that sentence.
     * "adhesive capsulitis" carries no banned word, no digit and no capital letter,
     * so the name, number, date and unit checks all pass it through. The banned list
     * catches the word "diagnosis" and not a diagnosis.
     *
     * A word the banned list already names is left to it, so that "diagnosis", which
     * ends in one of the endings below, is reported once and as the word it is.
     */
    private fun conditions(piece: String, text: String): List<Pair<ReadingFault, String>> =
        conditionNamesIn(piece)
            .filterNot { Words.says(text, it) }
            .distinct()
            .map { ReadingFault.InventedCondition to "\"$it\" is not named in the document" }

    /**
     * Every number traces back to the text, whether it was written in digits or in
     * words.
     *
     * A word passes on its value or on itself, which is what lets an honest plain
     * line say "four out of five" for a document's "MMT 4/5" while a number the page
     * never carries still fails. It is job 6's rule and it is here for job 6's
     * reason: the looser reading would need the validator to do arithmetic, and a
     * validator that computes is a second thing that can be wrong.
     */
    private fun numbers(piece: String, text: String): List<Pair<ReadingFault, String>> {
        val allowed = SummaryValidator.digitsIn(text) + spelledNumbersIn(text).map { it.second }
        val digits = SummaryValidator.digitsIn(piece)
            .filter { number -> allowed.none { abs(it - number) < CLOSE_ENOUGH } }
            .map { ReadingFault.InventedNumber to "$it is not in the document" }
        val spelled = spelledNumbersIn(piece)
            .filterNot { (written, value) ->
                allowed.any { abs(it - value) < CLOSE_ENOUGH } || Words.says(text, written)
            }
            .map { ReadingFault.InventedNumber to "\"${it.first}\" is not in the document" }
        return digits + spelled
    }

    /**
     * Months, seasons and weekdays. It overlaps the proper noun check on purpose,
     * because a season is lowercase and a month at the start of a sentence is
     * invisible to that check.
     */
    private fun dates(piece: String, text: String): List<Pair<ReadingFault, String>> =
        DATE_WORDS
            .filter { Words.says(piece, it) && !Words.says(text, it) }
            .map { ReadingFault.InventedDate to "$it is not in the document" }

    /** A measurement is a number and a unit, and the unit has to come from the page. */
    private fun measurements(piece: String, text: String): List<Pair<ReadingFault, String>> =
        UNITS
            .filter { usesUnit(piece, it) && !usesUnit(text, it) }
            .map { ReadingFault.InventedMeasurement to "$it is not a unit the document uses" }

    /**
     * The banned list, run over the part of a piece the app is actually saying.
     *
     * Where the line is, and AI.md draws it itself: "If a report says a range of
     * motion decreased, the app says the report says it, and nothing more." So the
     * rule is not the word, it is whose sentence the word is in. Inside quotation
     * marks the words are the document's and this list does not fire on them.
     * Outside them the words are the app's own and it does.
     *
     * "You should bring the sling" is the app advising. The note's line reads
     * "should continue the home programme" is the app reporting an instruction it
     * did not give, and the distance between those two sentences is the whole of
     * ADDENDUM-03 Part 7. Without the licence the plain words section fails on any
     * document that gives an instruction or characterises anything, which is most of
     * them, and the person gets "The app could only pull out the words this time"
     * every time while the feature looks as though it works.
     *
     * The licence is only worth having while the quotation is real, which is what
     * [quotesTheDocument] checks and why [ReadingFault.FabricatedQuote] exists: a
     * quoted span the page does not carry is left standing here, so a banned word
     * smuggled inside invented quotation marks fires exactly as it would bare.
     *
     * What this cannot check is the frame around the quotation. The marks are read
     * as attribution, so a bare quoted line with nothing said around it passes. That
     * is the narrowest thing it can be wrong about, and what passes is still the
     * document's own words in the document's own order.
     */
    private fun forbidden(piece: String, text: String): List<Pair<ReadingFault, String>> =
        bannedWordsIn(unquoted(piece, text)).map { ReadingFault.ForbiddenWord to "uses \"$it\"" }

    /** Quotation marks around words the page does not carry. */
    private fun quotations(piece: String, text: String): List<Pair<ReadingFault, String>> =
        QUOTED.findAll(piece)
            .map { it.groupValues[1] }
            .filterNot { quotesTheDocument(it, text) }
            .map { ReadingFault.FabricatedQuote to "\"$it\" is not what the document says" }
            .toList()

    /**
     * The piece with every quotation of the document taken out of it.
     *
     * Taken out where it stands, marks and all, rather than by looking for its words
     * anywhere in the piece. The difference matters, because a paragraph that reads
     * the note's improved line inside quotation marks and then says your flexion
     * improved in its own next sentence has done both, and may only do the first.
     */
    private fun unquoted(piece: String, text: String): String =
        QUOTED.replace(piece) {
            if (quotesTheDocument(it.groupValues[1], text)) " " else it.value
        }

    private fun flattened(text: String): String =
        text.lowercase().replace(NOT_PLAIN, " ").trim()

    /**
     * AI.md rejects the sentence, so the sentence is what the detail names, and the
     * piece it sits in is what gets dropped. The validator does not cut the sentence
     * out and keep the rest, because editing the model's paragraph would make this a
     * second author instead of a check.
     */
    private fun claims(piece: String): List<Pair<ReadingFault, String>> =
        Words.sentences(piece).flatMap { sentence ->
            causalPhrasesIn(sentence).map {
                ReadingFault.CausalClaim to "\"$sentence\" turns on \"$it\""
            }
        }

    /**
     * The lines of Part 7 that no single banned word gives away.
     *
     * One pass over the five lists rather than five checks, because a sentence that
     * trips two of them has done two things and the person reading the verdict should
     * see both. Whole pieces are dropped and never edited, as everywhere else here.
     *
     * Four of the five lines run on the same quotation licence [forbidden] explains,
     * and [Part7Line] says which one does not and why.
     */
    private fun part7(piece: String, text: String): List<Pair<ReadingFault, String>> {
        val said = unquoted(piece, text)
        return PART_7.flatMap { line ->
            line.words
                .filter { Words.saysStem(if (line.aboutVoice) said else piece, it) }
                .map { line.fault to "\"$it\" ${line.does}" }
        }
    }

    private fun <T> verdict(piece: T, found: List<Pair<ReadingFault, String>>): PieceVerdict<T> =
        PieceVerdict(
            piece = piece,
            faults = found.map { it.first }.distinct(),
            detail = found.map { it.second },
        )

    /**
     * The runs of number words in a piece, in the order they were written.
     *
     * A run ends at the first word that is not a number word, which is why "three
     * out of ten" is two numbers and not thirteen. "And" is the one word allowed
     * inside a run, and only where English puts it, after "hundred".
     */
    private fun spelledRuns(text: String): List<List<String>> {
        val runs = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        WORD.findAll(text).forEach { match ->
            val word = match.value.lowercase()
            val held = word == "and" && valueOf(current.lastOrNull().orEmpty()) == HUNDRED
            when {
                valueOf(word) != null -> current.add(word)
                held -> Unit
                current.isNotEmpty() -> {
                    runs.add(current)
                    current = mutableListOf()
                }
            }
        }
        if (current.isNotEmpty()) runs.add(current)
        return runs
    }

    private fun composed(run: List<String>): Double =
        run.fold(0.0) { total, word ->
            val value = valueOf(word) ?: 0.0
            if (value == HUNDRED) maxOf(total, 1.0) * HUNDRED else total + value
        }

    private fun valueOf(word: String): Double? =
        SummaryValidator.spelledWords[word.lowercase()]?.toDouble()

    /**
     * An abbreviation, by the only shape a note gives one: two letters or more, with
     * capitals at the front.
     *
     * Not "every letter is a capital", which is what this asked for first and which
     * missed "ADLs". The plural s is how a note writes it, and "What does the note
     * mean by ADLs?" is one of the commonest honest questions a person has of a
     * letter like this one. Under the older reading that question was grounded on
     * nothing and thrown away, because every other word in it is too short or too
     * ordinary to count.
     */
    private fun isAbbreviation(word: String): Boolean =
        word.length >= SHORTEST_ABBREVIATION &&
            word.take(SHORTEST_ABBREVIATION).all { it.isUpperCase() }

    /**
     * Units are matched on a letter boundary rather than [Words.says]'s word
     * boundary, because a report writes "120mm" as readily as "120 mm" and a digit in
     * front of a unit should not hide it.
     */
    private fun usesUnit(text: String, unit: String): Boolean =
        Regex("(?<!\\p{L})${Regex.escape(unit)}(?!\\p{L})", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)

    /** What "hundred" does to the run in front of it, which is all the sums there are. */
    private const val HUNDRED = 100.0

    /** The marks that can stand between a full stop and the sentence after it. */
    private const val CLOSERS = "\"\u201D')]"

    private const val SENTENCE_ENDS = ".!?"

    private val WORD = Regex("\\p{L}+")

    /** Everything that is not a letter or a digit, which is what a quotation may differ by. */
    private val NOT_PLAIN = Regex("[^\\p{L}\\p{N}]+")

    /**
     * A quoted span, in the shape [SummaryValidator.quotesIn] matches it. Held here
     * because this file needs where the span stands and not only the words in it.
     */
    private val QUOTED = Regex("[\u201C\"]([^\u201D\"]+)[\u201D\"]")

    /** The words an expansion puts between its own initials. */
    private val JOINING_WORDS = setOf("a", "an", "and", "for", "in", "of", "on", "or", "the", "to")

    /** A comma, then "so": the join that says one thing followed from another. */
    private val SO_JOIN = Regex(",\\s+so\\s")

    /**
     * One line of Part 7, the words that give it away, and what they are doing.
     *
     * [aboutVoice] says whether the line is about how the app speaks or about what a
     * reading is allowed to be about at all. Four of the five are about the voice, so
     * a quotation of the page is not the app saying it and the words inside quotation
     * marks are skipped, the same licence [forbidden] runs on. Scope is not about the
     * voice: a letter that mentions medication is a letter this app stays off, and
     * quoting the sentence is still handling it, so that line reads the piece whole.
     */
    private data class Part7Line(
        val fault: ReadingFault,
        val does: String,
        val words: List<String>,
        val aboutVoice: Boolean = true,
    )

    private val PART_7 = listOf(
        Part7Line(ReadingFault.Judgement, "says how it is going", judgements),
        Part7Line(ReadingFault.Advice, "tells the person what to do", advice),
        Part7Line(ReadingFault.PopulationClaim, "measures against people", population),
        Part7Line(ReadingFault.SecondGuess, "second guesses the clinician", secondGuesses),
        Part7Line(
            ReadingFault.OutOfScope,
            "is outside movement and care",
            outOfScope,
            aboutVoice = false,
        ),
    )

    private val DATE_WORDS = listOf(
        "January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December",
        "spring", "summer", "autumn", "winter", "fall",
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
    )

    private val UNITS = listOf(
        "degrees", "degree", "cm", "mm", "mmhg", "ml", "metres", "metre",
        "meters", "meter", "inches", "inch", "feet", "kg", "kilograms",
        "lb", "lbs", "pounds", "seconds", "minutes", "percent", "grade",
        "reps", "sets",
    )

    /**
     * The endings a condition name takes in English. "capsulitis", "stenosis",
     * "tendinopathy", "neuralgia", "hemiplegia". Nothing an honest reading says has
     * this shape unless the page said it first.
     */
    private val CONDITION_ENDINGS = listOf("itis", "osis", "opathy", "algia", "plegia")

    /**
     * The common conditions with no ending to catch them by.
     *
     * "syndrome" stands for every condition it ends, because a reading that has
     * reached the word has named one whatever came in front of it, and "torn" is here
     * beside "tear" because that is how a shoulder gets described.
     */
    private val CONDITION_NAMES = listOf(
        "frozen shoulder", "sprain", "strain", "tear", "torn", "rupture",
        "fracture", "impingement", "sciatica", "stroke", "asthma", "diabetes",
        "dementia", "cancer", "hypertension", "syndrome", "whiplash",
        "slipped disc", "concussion", "vertigo", "palsy", "atrophy",
    )

    /**
     * Ordinary words that turn up capitalised when the sentence splitter cuts in the
     * wrong place, at "Dr." or "e.g.". The app's own paragraphs never capitalise any
     * of them mid sentence on purpose, so nothing is lost by letting them through.
     */
    private val ORDINARY_CAPITALS = setOf(
        "A", "An", "The", "This", "That", "These", "Those", "It", "Its", "I",
        "If", "In", "Is", "Are", "You", "Your", "Yours", "They", "Their", "Them",
        "There", "We", "Our", "Us", "He", "She", "His", "Her", "Him", "No",
        "Not", "Yes", "OK", "Okay", "And", "But", "So", "When", "What", "Who",
        "Why", "How", "Ask", "Do", "Does", "Did", "Could", "Would", "Will",
    )

    /**
     * Words long enough to pass the length test without saying anything about this
     * particular document. A question grounded only on one of these is grounded on
     * nothing.
     */
    private val QUESTION_STOPWORDS = setOf(
        "about", "again", "against", "another", "anything", "asked", "asking",
        "before", "being", "could", "doing", "during", "every", "everything",
        "going", "might", "other", "others", "please", "really", "since",
        "someone", "something", "still", "their", "there", "these", "thing",
        "things", "those", "until", "usually", "wanted", "whether", "which",
        "while", "would", "yours", "means", "meant", "should", "supposed",
    )
}
