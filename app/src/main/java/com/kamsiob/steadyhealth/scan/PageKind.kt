package com.kamsiob.steadyhealth.scan

/**
 * What a clue on the page points at.
 *
 * Three of the five are out of scope, and they are named separately rather than
 * collapsed into one because the saved page is labelled and because somebody
 * reading a bug report needs to know which of the three fired.
 */
enum class Pointing(val outOfScope: Boolean = false) {
    Exercises,
    ReportOrLetter,
    LabWork(outOfScope = true),
    Imaging(outOfScope = true),
    Medicines(outOfScope = true),
}

/**
 * One thing found on the page, named so the classification can be read back.
 *
 * ADDENDUM-03 Part 5 asks the app to say what it is looking at, and a screen that
 * can only say "exercises" is harder to trust than one that can say it counted
 * repetitions and saw movement names. These names are also what the tests assert
 * on, which is why every one of them is a thing a person could point to on the
 * paper rather than a score.
 */
enum class Signal(val points: Pointing) {

    /** "3 sets of 10", "3 x 10", "12 reps". The most reliable mark of a sheet. */
    RepsAndSets(Pointing.Exercises),

    /** "twice a day", "2 x per day", "Frequency:". How often, which prose rarely says. */
    TimesPerDay(Pointing.Exercises),

    /** "hold for 10 seconds". A duration attached to a position. */
    HoldFor(Pointing.Exercises),

    /** A movement the app or a therapist would name. See the list in Clues. */
    MovementName(Pointing.Exercises),

    /** Three or more lines that start with a number or a bullet. A sheet is a list. */
    NumberedSteps(Pointing.Exercises),

    /** "each side", "both legs". Said to somebody doing the movement, not reading about it. */
    EachSide(Pointing.Exercises),

    /** "Dear", "To whom it may concern", "Re:". A letter opens by addressing somebody. */
    Salutation(Pointing.ReportOrLetter),

    /** "Sincerely", "Kind regards", "thank you for the referral". A letter closes too. */
    SignOff(Pointing.ReportOrLetter),

    /** A clinician's title or discipline. On its own this is often only a letterhead. */
    ClinicianTitle(Pointing.ReportOrLetter),

    /** Two or more of ROM, MMT, gait, transfers, AROM, PROM, WNL and their neighbours. */
    ClinicalShorthand(Pointing.ReportOrLetter),

    /** "Date of service", "Initial evaluation", "Discharge date". */
    DateOfService(Pointing.ReportOrLetter),

    /** Two or more long sentences. This is what actually makes a page a report. */
    ProseParagraphs(Pointing.ReportOrLetter),

    /** The name of something measured in blood or urine. */
    LabAnalyte(Pointing.LabWork),

    /** "Reference range", or a bracketed low to high pair. */
    LabReferenceRange(Pointing.LabWork),

    /** mg/dL, mmol/L and the rest. Units that only appear beside a measured value. */
    LabUnits(Pointing.LabWork),

    /** "Specimen", "Collected", "Serum". The paperwork around a sample. */
    LabSpecimen(Pointing.LabWork),

    /** MRI, x-ray, ultrasound, CT and the rest. */
    ImagingModality(Pointing.Imaging),

    /** "Impression", "Technique", "Comparison". The headings a radiology page uses. */
    ImagingSection(Pointing.Imaging),

    /** "Radiologist", "Department of radiology". Who wrote it. */
    ImagingAuthor(Pointing.Imaging),

    /** Tablet, capsule, inhaler. The physical form of a medicine. */
    MedicineForm(Pointing.Medicines),

    /** A dose: a number followed by mg, mcg, mL or units. */
    MedicineDose(Pointing.Medicines),

    /** "Refills", "Pharmacy", "Current medications". The list around the medicine. */
    MedicineDispensing(Pointing.Medicines),

    /** "By mouth", "at bedtime", "as needed for". How a medicine is taken. */
    MedicineRoute(Pointing.Medicines),
}

/**
 * What the app decided a photographed page is. ADDENDUM-03 Part 5, LOGIC.md 17.
 *
 * Part 5 gives five outcomes and one question per outcome, so this gives five
 * results and no strings. The screen owns the words, the way Adaptation and
 * Noticed are owned by their screens, because the same decision has to be
 * askable in a sentence today and in a different sentence after somebody reads
 * it out loud in a hallway.
 *
 * Two things are deliberate and both cost accuracy on purpose.
 *
 * Unclear is a first-class answer, not a failure. Part 5 has a good screen for
 * it that offers both paths and "Just keep it", so an honest shrug lands
 * somewhere useful, and a confident wrong answer sends somebody into a flow that
 * cannot work on the page in their hand. Two clues are needed before the app
 * will name anything, and thin pages come back Unclear.
 *
 * Out of scope overrules. A blood test, an imaging page and a medication list
 * must never be handed to the reader in Part 7, so what the page looks like is
 * worked out first and the out of scope clues are then allowed to overrule it. A
 * single one of them is enough on any page the app was not going to name anyway,
 * because Unclear is not a refusal: Part 5's unclear screen offers the reader as
 * one of its two buttons, so a lab page that came back Unclear would still reach
 * the reader. Being wrong in that direction costs the person one sentence.
 *
 * Every clue carries its own name into the result, so a page can be argued with
 * rather than only disagreed with.
 */
sealed interface PageKind {

    /** Every clue that fired, in the order the lists declare them. */
    val signals: List<Signal>

    /** A therapist's sheet. Part 5 offers to add them, which is Part 6. */
    data class Exercises(override val signals: List<Signal>) : PageKind

    /** Something written about the person rather than for them. Part 5 offers Part 7. */
    data class ReportOrLetter(override val signals: List<Signal>) : PageKind

    /** Both offers, in Part 5's order: the exercises first, then the reading. */
    data class Both(override val signals: List<Signal>) : PageKind

    /** The evidence was thin. Part 5 asks the person instead of guessing at them. */
    data class Unclear(override val signals: List<Signal>) : PageKind

    /**
     * Something the app will keep but will not read.
     *
     * [pointsAt] is whichever of the three fired most, and it is here for the log
     * and for a bug report rather than for the screen. Part 5 gives this outcome
     * exactly one sentence and that sentence does not name the kind of page, so a
     * screen that read this aloud would be the app telling somebody what their own
     * document is. Nothing downstream is allowed to treat it as a finding.
     */
    data class OutOfScope(
        val pointsAt: Pointing,
        override val signals: List<Signal>,
    ) : PageKind

    companion object {

        /** Two clues before the app will name what it is looking at. One is a coincidence. */
        const val ENOUGH_TO_SAY = 2

        /**
         * Clues from one out of scope family before the page is out of scope
         * outright, whatever else is on it.
         *
         * Counted inside a family rather than across all three, which is the
         * difference between a page that is a blood test and a page that borrowed
         * two ordinary words. A real lab page fires several lab clues and a real
         * medication list fires several medicine ones, so nothing genuinely out of
         * scope is lost by asking for two of a kind. What is gained is that an
         * exercise sheet reading "Technique: keep your knee over your toes" and
         * "ice it as needed for soreness" is still an exercise sheet, rather than
         * one imaging word plus one medicine word adding up to a scan.
         */
        const val ENOUGH_TO_STAY_OUT = 2

        /** Under this many words there is nothing to classify, only a guess to make. */
        const val ENOUGH_WORDS = 12

        /** Lines starting with a number or a bullet before the page counts as a list. */
        const val ENOUGH_STEPS = 3

        /** Distinct pieces of shorthand before that counts. One "gait" is not a report. */
        const val ENOUGH_SHORTHAND = 2

        /** Long sentences before the page counts as prose. */
        const val ENOUGH_SENTENCES = 2

        /** Words in a sentence before it is prose rather than a heading or an item. */
        const val LONG_SENTENCE = 10

        /**
         * Classify one page of extracted text.
         *
         * The order is the safety order, not the likelihood order. What the page
         * looks like is worked out first and out of scope is then allowed to
         * overrule it. Doing it the other way round reads as the safer order and is
         * not, because the question the overrule has to be able to answer is
         * whether the page was going to be named at all.
         */
        fun of(text: String): PageKind {
            val page = Page(text)
            val fired = Clues.all.filter { it.fires(page) }.map(Clue::signal)
            val looks = looksLike(page, fired)
            val out = fired.filter { it.points.outOfScope }
            if (out.isEmpty()) return looks

            // Two of a kind, or one of anything on a page the app was not going to
            // name. Unclear is the second half of that on purpose: Part 5's unclear
            // screen offers the reader as one of its buttons, so an unnamed page
            // with a lab word on it is still a page somebody can ask the app to
            // explain, and that is the outcome this exists to prevent.
            val family = worstOf(out)
            val ofAKind = out.count { it.points == family }
            return if (ofAKind >= ENOUGH_TO_STAY_OUT || looks is Unclear) {
                OutOfScope(pointsAt = family, signals = fired)
            } else {
                looks
            }
        }

        /**
         * What the page looks like, before out of scope gets a say.
         *
         * Prose is required rather than counted. A clinic name and a date at the top
         * of an exercise sheet is a letterhead, and a letterhead is not a letter;
         * what makes a page a report is that somebody wrote sentences.
         */
        private fun looksLike(page: Page, fired: List<Signal>): PageKind {
            if (page.words < ENOUGH_WORDS) return Unclear(fired)
            val exercises = fired.count { it.points == Pointing.Exercises }
            val report = fired.filter { it.points == Pointing.ReportOrLetter }
            val looksLikeExercises = exercises >= ENOUGH_TO_SAY
            val looksLikeReport =
                report.size >= ENOUGH_TO_SAY && Signal.ProseParagraphs in report
            return when {
                looksLikeExercises && looksLikeReport -> Both(fired)
                looksLikeExercises -> Exercises(fired)
                looksLikeReport -> ReportOrLetter(fired)
                else -> Unclear(fired)
            }
        }

        /**
         * Which of the three out of scope kinds to label the page with.
         *
         * The one with the most clues, and on a tie the one declared first, which
         * puts blood work ahead of imaging ahead of medicines. This also decides
         * which family has to reach [ENOUGH_TO_STAY_OUT], so the strongest one is
         * the one that gets to speak; the tie itself only picks a label.
         */
        private fun worstOf(out: List<Signal>): Pointing =
            Pointing.entries.filter { it.outOfScope }
                .maxBy { family -> out.count { it.points == family } }
    }
}

/** One clue: a named signal and the question that decides whether it fired. */
private class Clue(val signal: Signal, val fires: (Page) -> Boolean)

/**
 * The page, prepared once so that every clue below reads as one line.
 *
 * Everything is lowercased here rather than in twenty places, and the derived
 * counts are computed once because a clue that is expensive to check is a clue
 * somebody will be tempted to leave out.
 */
private class Page(raw: String) {

    val body: String = raw.lowercase()

    val lines: List<String> = body.lines().map(String::trim).filter(String::isNotEmpty)

    val words: Int = body.split(WHITESPACE).count(String::isNotBlank)

    /** Lines that open with a number or a bullet, which is what a sheet is made of. */
    val steps: Int = lines.count { LIST_ITEM.containsMatchIn(it) }

    /**
     * Sentences long enough to be prose.
     *
     * List items are dropped first so that a sheet with a long instruction on it
     * does not read as a paragraph, and the remaining lines are joined before
     * splitting, because OCR breaks a letter's sentences across lines and a
     * per-line count would find no prose anywhere.
     *
     * A sentence has to be finished to count. Joining lines turns a letterhead and
     * an address and an appointment time into one long run of words, and without
     * the full stop at the end that run reads as a paragraph when it is a header.
     */
    val sentences: Int = SENTENCE.findAll(
        lines.filterNot { LIST_ITEM.containsMatchIn(it) }.joinToString(" "),
    ).count { it.value.split(WHITESPACE).count(String::isNotBlank) >= PageKind.LONG_SENTENCE }

    /** How many of these phrases the page holds, counted as whole words. */
    fun countOf(phrases: List<String>): Int = phrases.count { holds(it) }

    fun holds(phrase: String): Boolean = wordish(phrase).containsMatchIn(body)

    fun matches(pattern: Regex): Boolean = pattern.containsMatchIn(body)

    private companion object {
        val WHITESPACE = Regex("\\s+")
        val SENTENCE = Regex("[^.?!]+[.?!]")
        val LIST_ITEM = Regex("^(\\d+[.)]|\\(\\d+\\)|[-*•])\\s")
    }
}

/**
 * Whole word or phrase matching, because the shorthand is short.
 *
 * Without this "rom" matches "from", "prn" matches nothing useful and "adl"
 * matches "adlington". The boundary is letters and digits rather than the usual
 * word boundary so that "x-ray" and "mg/dl" still match as written.
 */
private fun wordish(phrase: String): Regex =
    Regex("(?<![a-z0-9])" + Regex.escape(phrase) + "(?![a-z0-9])")

/**
 * The evidence, written out as named lists rather than as one pattern.
 *
 * These are three separate readable lists on purpose. Somebody who wants to know
 * why a page was classified the way it was should be able to read the list, find
 * the phrase, and stop; and somebody who wants to fix a miss should be able to
 * add one phrase to one list without touching anything else.
 */
private object Clues {

    /**
     * What an exercise sheet looks like. ADDENDUM-03 Part 5 names most of these:
     * repetition and set counts, movement names, "x per day", "hold for", and
     * numbered or bulleted lists of instructions.
     */
    val exercises: List<Clue> = listOf(
        Clue(Signal.RepsAndSets) { it.matches(REPS_AND_SETS) },
        Clue(Signal.TimesPerDay) { it.matches(TIMES_PER_DAY) },
        Clue(Signal.HoldFor) { it.matches(HOLD_FOR) },
        Clue(Signal.MovementName) { it.countOf(MOVEMENT_NAMES) > 0 },
        Clue(Signal.NumberedSteps) { it.steps >= PageKind.ENOUGH_STEPS },
        Clue(Signal.EachSide) { it.countOf(EACH_SIDE) > 0 },
    )

    /**
     * What a report or a letter looks like: prose, a salutation, a clinician's
     * name or title, clinical shorthand, and dates of service.
     *
     * The shorthand needs two distinct terms before it counts. "Gait training" is
     * a line on plenty of exercise sheets, and one borrowed word is not a report.
     */
    val report: List<Clue> = listOf(
        Clue(Signal.Salutation) { it.matches(SALUTATION) },
        Clue(Signal.SignOff) { it.countOf(SIGN_OFF) > 0 },
        Clue(Signal.ClinicianTitle) { it.countOf(CLINICIAN_TITLES) > 0 },
        Clue(Signal.ClinicalShorthand) { it.countOf(SHORTHAND) >= PageKind.ENOUGH_SHORTHAND },
        Clue(Signal.DateOfService) { it.countOf(SERVICE_DATES) > 0 },
        Clue(Signal.ProseParagraphs) { it.sentences >= PageKind.ENOUGH_SENTENCES },
    )

    /**
     * What the app will not read. Part 5 names three: a lab page, imaging, and a
     * medication list.
     *
     * These are wider than the other two lists and that is the point. A miss here
     * sends a blood test to a model that explains documents, and the cost of a
     * false alarm is one sentence saying the page was saved but not read.
     */
    val outOfScope: List<Clue> = listOf(
        Clue(Signal.LabAnalyte) { it.countOf(ANALYTES) > 0 },
        Clue(Signal.LabReferenceRange) { it.countOf(REFERENCE_RANGE) > 0 || it.matches(LOW_TO_HIGH) },
        Clue(Signal.LabUnits) { it.countOf(LAB_UNITS) > 0 },
        Clue(Signal.LabSpecimen) { it.countOf(SPECIMEN) > 0 },
        Clue(Signal.ImagingModality) { it.countOf(MODALITIES) > 0 },
        Clue(Signal.ImagingSection) { it.countOf(IMAGING_SECTIONS) > 0 },
        Clue(Signal.ImagingAuthor) { it.countOf(IMAGING_AUTHORS) > 0 },
        Clue(Signal.MedicineForm) { it.countOf(MEDICINE_FORMS) > 0 },
        Clue(Signal.MedicineDose) { it.matches(DOSE) },
        Clue(Signal.MedicineDispensing) { it.countOf(DISPENSING) > 0 },
        Clue(Signal.MedicineRoute) { it.countOf(ROUTES) > 0 },
    )

    val all: List<Clue> = exercises + report + outOfScope

    /** "3 sets of 10", "3x10", "12 reps", "Reps: 15". */
    private val REPS_AND_SETS = Regex(
        "\\b\\d+\\s*[x×]\\s*\\d+\\b|\\b\\d+\\s*(sets?|reps?|repetitions?)\\b|" +
            "\\b(sets?|reps?|repetitions?)\\s*[:=]\\s*\\d+",
    )

    /** "twice a day", "3 x per day", "2 times daily", "Frequency:". */
    private val TIMES_PER_DAY = Regex(
        "\\b\\d+\\s*(x|times)\\s*(per|a|each)?\\s*(day|daily|week|weekly)\\b|" +
            "\\b(once|twice|three times)\\s*(a|per|each)\\s*(day|week)\\b|" +
            "\\bevery day\\b|\\bfrequency\\s*[:=]",
    )

    /** "hold for 10 seconds", "hold 30 sec", "10 seconds". */
    private val HOLD_FOR = Regex(
        "\\bhold\\s+(for\\s+)?\\d+|\\bhold\\s+(for|each|this)\\b|\\b\\d+\\s*(seconds?|secs?)\\b",
    )

    /**
     * Movements a therapist writes on a sheet.
     *
     * Kept here rather than read from the app's own movement library on purpose.
     * The library is the list of things the app can run, and a sheet is full of
     * movements the app has never heard of; a classifier tied to the library
     * would get quieter every time somebody's physio wrote something new.
     */
    private val MOVEMENT_NAMES = listOf(
        "sit to stand", "sit to stands", "sit-to-stand", "sit-to-stands",
        "heel raise", "heel raises", "calf raise", "calf raises", "toe raise", "toe raises",
        "wall push up", "wall push ups", "wall push-up", "wall push-ups",
        "squat", "squats", "mini squat", "mini squats",
        "bridge", "bridges", "glute bridge", "clam", "clams", "clamshell",
        "hamstring stretch", "calf stretch", "quad stretch", "quad set", "quad sets",
        "step up", "step ups", "step-up", "step-ups",
        "straight leg raise", "straight leg raises", "ankle pump", "ankle pumps",
        "hip abduction", "hip flexion", "knee extension", "knee bend", "knee bends",
        "shoulder press", "shoulder flexion", "wall slide", "wall slides",
        "marching", "march in place", "heel to toe", "heel-to-toe",
        "tandem stance", "single leg stance", "standing balance", "sidelying", "side lying",
        "bird dog", "seated row", "resistance band", "theraband",
    )

    private val EACH_SIDE = listOf(
        "each side", "each leg", "each arm", "each foot", "both sides", "both legs",
        "per side", "each direction", "other side", "opposite side",
    )

    /** "Dear Dr", "To whom it may concern", "Re:". */
    private val SALUTATION = Regex(
        "(?<![a-z0-9])(dear\\s+[a-z]|to whom it may concern|re\\s*:|regarding\\s*:)",
    )

    private val SIGN_OFF = listOf(
        "sincerely", "yours sincerely", "yours faithfully", "kind regards",
        "best regards", "warm regards", "thank you for the referral",
        "thank you for allowing", "please do not hesitate", "please feel free to contact",
    )

    private val CLINICIAN_TITLES = listOf(
        "physical therapist", "physiotherapist", "occupational therapist",
        "physical therapy", "physiotherapy", "occupational therapy",
        "dpt", "mpt", "pta", "otr/l", "ocs", "gcs", "cota",
        "referring physician", "attending physician", "primary care physician",
    )

    /**
     * The shorthand ADDENDUM-03 Part 5 lists, and its close neighbours.
     *
     * Every one of these is a word a clinician writes to another clinician. Two of
     * them together is the strongest sign that the page was not written for the
     * person holding it, which is the whole reason Part 7 exists.
     *
     * Flexion, extension and abduction are deliberately not here. They read as
     * shorthand but they are also how a therapist writes a movement on a sheet,
     * and a word that appears on both kinds of page is evidence for neither.
     */
    private val SHORTHAND = listOf(
        "rom", "arom", "prom", "aarom", "mmt", "wnl", "gait", "transfer", "transfers",
        "ambulation", "ambulates", "ambulatory", "adl", "adls", "hep", "poc",
        "plan of care", "antalgic", "proprioception", "wbat", "nwb", "cga",
        "min assist", "mod assist", "max assist", "independent with", "bil", "b/l",
        "palpation", "tenderness", "within functional limits",
    )

    private val SERVICE_DATES = listOf(
        "date of service", "dates of service", "visit date", "date of visit",
        "initial evaluation", "initial eval", "discharge date", "date seen",
        "treatment dates", "re-evaluation", "reevaluation", "progress note",
    )

    private val ANALYTES = listOf(
        "hemoglobin", "haemoglobin", "hba1c", "a1c", "creatinine", "glucose",
        "cholesterol", "ldl", "hdl", "triglycerides", "triglyceride", "tsh",
        "platelets", "platelet", "white blood cell", "wbc", "rbc", "hematocrit",
        "sodium", "potassium", "chloride", "bicarbonate", "urea", "bun", "egfr",
        "bilirubin", "albumin", "ferritin", "vitamin d", "vitamin b12", "psa",
        "esr", "crp", "inr", "alkaline phosphatase",
    )

    private val REFERENCE_RANGE = listOf(
        "reference range",
        "reference interval",
        "reference values",
        "normal range",
        "ref range",
        "out of range",
        "flag",
        "abnormal",
    )

    private val LAB_UNITS = listOf(
        "mg/dl", "mmol/l", "mmol/mol", "g/dl", "g/l", "iu/l", "u/l", "ng/ml",
        "pg/ml", "umol/l", "meq/l", "mcg/l", "10^9/l", "x10^9/l", "cells/ul",
    )

    private val SPECIMEN = listOf(
        "specimen", "collected", "collection date", "accession", "fasting",
        "serum", "plasma", "venipuncture", "ordering provider", "laboratory",
    )

    private val MODALITIES = listOf(
        "mri", "x-ray", "xray", "x ray", "radiograph", "radiographs", "ultrasound",
        "ct scan", "computed tomography", "magnetic resonance", "dexa", "dxa",
        "sonography", "fluoroscopy", "bone scan", "mammogram", "arthrogram",
    )

    private val IMAGING_SECTIONS = listOf(
        "impression", "technique", "comparison", "clinical indication", "contrast",
        "no acute", "unremarkable", "joint space narrowing", "effusion",
        "degenerative changes", "no fracture", "soft tissues",
    )

    private val IMAGING_AUTHORS = listOf(
        "radiologist",
        "radiology",
        "department of radiology",
        "electronically signed by",
    )

    private val MEDICINE_FORMS = listOf(
        "tablet", "tablets", "capsule", "capsules", "inhaler", "suppository",
        "ointment", "transdermal patch", "oral solution", "eye drops", "lozenge",
    )

    /** A number and a dose unit together. Neither half alone means anything. */
    private val DOSE = Regex("\\b\\d+(\\.\\d+)?\\s*(mg|mcg|ug|ml|units?|iu)\\b")

    private val DISPENSING = listOf(
        "refill", "refills", "pharmacy", "pharmacist", "prescription", "prescriber",
        "rx", "dispense", "quantity", "ndc", "medication list", "current medications",
        "active medications", "medications:", "take as directed", "do not crush",
    )

    private val ROUTES = listOf(
        "by mouth", "orally", "as needed for", "at bedtime", "with food",
        "with meals", "prn", "subcutaneous", "topically", "under the tongue",
    )

    /** A bracketed low to high pair, which is how a lab page prints its range. */
    private val LOW_TO_HIGH = Regex("\\(\\s*\\d+(\\.\\d+)?\\s*-\\s*\\d+(\\.\\d+)?\\s*\\)")
}
