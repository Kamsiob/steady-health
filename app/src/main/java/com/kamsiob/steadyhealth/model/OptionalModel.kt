package com.kamsiob.steadyhealth.model

/**
 * What the app does by hand when there is no model for it.
 *
 * ADDENDUM-03 Part 7 and DESIGN.md section 7: "the app is fully usable with no model
 * installed, and the model screen never implies the person is missing out." Every job
 * below names one of these, and the type system is what holds that promise: a job
 * cannot be declared without saying how it is done without help, so there is no way
 * to add a job that only exists when a download has finished.
 */
enum class ManualPath(val id: String) {

    /** The person types what they want to be able to do, and picks a domain for it. */
    TypeIt("type_it"),

    /** The person taps tags from the fixed vocabulary in AI.md job 2. */
    PickTags("pick_tags"),

    /** The week is there to look at whether or not anything wrote about it. */
    LookAtTheWeek("look_at_the_week"),

    /** The photo of the document is kept and viewable, which is the whole document. */
    LookAtThePhoto("look_at_the_photo"),
}

/**
 * One job a model does, and the way that job is done without it.
 *
 * From AI.md and ADDENDUM-03 Part 7. This is the declarative capability metadata that
 * standards/kamsiob-project-template.md section C7 asks for: the screen lists what a
 * download would change by reading these, rather than carrying a hardcoded sentence
 * per model that goes stale the first time a job moves.
 */
enum class ModelJob(val id: String, val manual: ManualPath) {

    /** AI.md job 1. Spoken or typed words become tracked items the person confirms. */
    WordsToTrackedItems("words_to_tracked_items", ManualPath.TypeIt),

    /** AI.md job 2. Tags picked out of a note, from a fixed vocabulary. */
    TagWhatYouWrote("tag_what_you_wrote", ManualPath.PickTags),

    /** AI.md job 3. The two short paragraphs about the week. */
    WriteTheWeekNote("write_the_week_note", ManualPath.LookAtTheWeek),

    /** ADDENDUM-03 Part 7. The document explained in plain words, and its terms. */
    ReadADocument("read_a_document", ManualPath.LookAtThePhoto),
}

/**
 * The licence a set of weights arrives under, and whether it has to be accepted first.
 *
 * ADDENDUM-03 Part 7 under LICENSING: HAI-DEF terms are shown before the download and
 * accepted there, which makes acceptance a step in the flow rather than a line in a
 * settings screen. It is a property of the licence and not of the model so that the
 * screen asks the licence rather than checking which model it is holding.
 */
enum class Licence(val id: String, val termsAcceptedBeforeDownload: Boolean) {

    /** Gemma's terms. Apache-2.0, nothing to accept in the app. */
    Apache2("apache_2_0", termsAcceptedBeforeDownload = false),

    /** Google's Health AI Developer Foundations terms, which prohibit Clinical Use. */
    HaiDef("hai_def", termsAcceptedBeforeDownload = true),
}

/**
 * The two optional models, and everything the decision layer knows about them.
 *
 * ADDENDUM-03 Part 7, "TWO MODELS, ONE CHOICE", and AI.md. Two downloads, either or
 * both or neither, and neither is required for anything. There is deliberately no
 * third entry standing for "nothing extra": nothing extra is the absence of these,
 * not a lesser member of the same list, and giving it an entry here is how a screen
 * ends up drawing it as the first of three options with the other two above it.
 *
 * The identifier and the size are the numbers ADDENDUM-03 says to verify at build
 * time rather than trust from the specification. They are constants with that note on
 * them so that verifying is an edit to two lines, and so a test can say what the app
 * currently believes.
 */
enum class OptionalModel(
    val id: String,
    /** The weights this names. To be re-verified at build time. See Sizes. */
    val weights: String,
    val bytes: Long,
    val licence: Licence,
    val does: Set<ModelJob>,
) {

    /**
     * Gemma 4 E4B. The person's own words become what the app tracks.
     *
     * The first model somebody meets, and the one that changes the most screens: it
     * covers three of the four jobs, so removing it falls back to three manual paths
     * rather than one.
     */
    YourOwnWords(
        id = "your_own_words",
        weights = "gemma-4-e4b-it",
        bytes = Sizes.ABOUT_TWO_AND_A_HALF_GB,
        licence = Licence.Apache2,
        does = setOf(
            ModelJob.WordsToTrackedItems,
            ModelJob.TagWhatYouWrote,
            ModelJob.WriteTheWeekNote,
        ),
    ),

    /**
     * MedGemma 1.5 4B. Reads and explains reports and letters from a therapist.
     *
     * One job, and the one job where a medical model earns its place, because the
     * vocabulary is the entire difficulty. It arrives under HAI-DEF rather than
     * Apache-2.0, which is why acceptance is part of its download and not of the
     * other one.
     */
    DocumentsFromYourTherapist(
        id = "documents_from_your_therapist",
        weights = "medgemma-1.5-4b-it",
        bytes = Sizes.ABOUT_TWO_AND_A_HALF_GB,
        licence = Licence.HaiDef,
        does = setOf(ModelJob.ReadADocument),
    );

    /** The manual paths that do this model's work when it is not on the phone. */
    val byHand: Set<ManualPath> get() = does.map { it.manual }.toSet()

    /**
     * The sizes the app currently believes, kept apart so the note travels with them.
     *
     * ADDENDUM-03 Part 7: "Verify the current identifier, size, and recommended
     * on-device path at build time." Both are about two and a half gigabytes and
     * together about five, which is the number the whole screen exists because of.
     * Bytes are decimal here, matching the way a phone reports its free space, so
     * that a size and a free space subtract to something a person would recognise.
     *
     * TO BE RE-VERIFIED AT BUILD TIME. If a published size differs, change it here
     * and nowhere else, and the tests in
     * ModelRoomTest will say what changed.
     */
    object Sizes {
        const val ABOUT_TWO_AND_A_HALF_GB = 2_500_000_000L
    }
}
