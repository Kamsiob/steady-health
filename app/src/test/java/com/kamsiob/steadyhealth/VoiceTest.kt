package com.kamsiob.steadyhealth

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The voice rules in DESIGN.md section 6, checked against every string the app
 * actually ships rather than against good intentions.
 *
 * DESIGN.md lists words that do not exist in this app. That is a promise about
 * the finished software, not a note for whoever is typing, and a promise nothing
 * checks is a promise that quietly stops being true. So this reads the real
 * resource files, the ones that end up in the APK, and fails the build if a
 * banned word gets in.
 *
 * The banned list is restated here in this file's own words on purpose. An
 * independent restatement is what catches somebody editing the shared list in the
 * app and the test agreeing with them.
 */
class VoiceTest {

    /**
     * Every string the app can show, from every file it keeps them in.
     *
     * The cards live in their own file because they are long-form copy from
     * CONTENT.md rather than interface labels, and reading only strings.xml would
     * have left the longest text in the app unchecked.
     */
    private val strings: Map<String, String> by lazy {
        readStrings("strings.xml") + readStrings("cards.xml")
    }

    @Test
    fun noStringUsesAWordTheAppDoesNotHave() {
        val offences = strings.flatMap { (name, value) ->
            BANNED
                .filter { it.matches(value) }
                .filterNot { name in ALLOWED_ANYWAY[it.word].orEmpty() }
                .map { "$name uses \"${it.word}\": $value" }
        }
        assertThat(offences).isEmpty()
    }

    @Test
    fun everyBannedWordExceptionIsExactlyWhereItIsSupposedToBe() {
        // Pinned both ways. A string named here that has stopped using the word is
        // an exception nobody needs any more, and leaving it standing is how a
        // list of exceptions turns into a list of excuses.
        ALLOWED_ANYWAY.forEach { (word, names) ->
            val banned = Banned(word)
            names.forEach { name ->
                val value = strings[name]
                assertWithMessage("$name is listed as an exception for \"$word\"")
                    .that(value)
                    .isNotNull()
                assertWithMessage("$name still needs its exception for \"$word\"")
                    .that(banned.matches(value.orEmpty()))
                    .isTrue()
            }
        }
    }

    @Test
    fun noStringUsesADash() {
        // The universal standard: never an em dash anywhere a person reads. The
        // en dash goes with it, because lint asks for one in page ranges and the
        // house rule is that neither appears.
        val offences = strings.filterValues { it.contains('—') || it.contains('–') }
        assertThat(offences.keys).isEmpty()
    }

    @Test
    fun noStringShouts() {
        val offences = strings.filterValues { it.contains('!') }
        assertThat(offences.keys).isEmpty()
    }

    @Test
    fun everyStringIsSentenceCase() {
        // DESIGN.md section 2: sentence case everywhere, no uppercase labels.
        //
        // Two things a naive version of this gets wrong, both found by running it.
        // Arabic has no case at all, so `uppercase()` returns the same string and
        // every Arabic label looks like shouting. And a proper name carrying an
        // acronym is not a shouted label; PAR-Q+ is the questionnaire's name.
        val offences = strings.filterValues { value ->
            value.split(' ').any { word ->
                val letters = word.filter { it.isLetter() }
                val hasCase = letters.lowercase() != letters.uppercase()
                hasCase &&
                    letters.length >= SHOUT &&
                    letters == letters.uppercase() &&
                    PROPER_NAMES.keys.none { value.contains(it) }
            }
        }
        assertThat(offences.keys).isEmpty()
    }

    @Test
    fun theProperNameExceptionIsExactlyWhereItIsSupposedToBe() {
        // Pinned, so the exception cannot spread into ordinary copy.
        PROPER_NAMES.forEach { (name, allowed) ->
            val using = strings.filterValues { it.contains(name) }.keys
            assertWithMessage("\"$name\" appears only where it is allowed to")
                .that(using)
                .containsExactlyElementsIn(allowed)
        }
    }

    @Test
    fun thereIsSomethingToCheck() {
        // A parser that silently matches nothing would make every test above pass.
        assertThat(strings.size).isAtLeast(TEN)
    }

    private fun readStrings(fileName: String): Map<String, String> {
        val file = findResourceFile(fileName)
        val text = file.readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

        val singles = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text)
            .associate { it.groupValues[1] to it.groupValues[2] }

        // Plurals hold real sentences too, and a check that only reads <string>
        // would let a whole quantity form through.
        val plurals = Regex("""<plurals name="([^"]+)"[^>]*>(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text)
            .flatMap { plural ->
                Regex("""<item quantity="([^"]+)"[^>]*>(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(plural.groupValues[2])
                    .map { "${plural.groupValues[1]}:${it.groupValues[1]}" to it.groupValues[2] }
            }
            .toMap()

        return singles + plurals
    }

    /** Walk up from wherever the test runs until the module is found. */
    private fun findResourceFile(fileName: String): File {
        val relative = "src/main/res/values/$fileName"
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative)
            if (candidate.exists()) return candidate
            val inModule = File(dir, "app/$relative")
            if (inModule.exists()) return inModule
            dir = dir.parentFile
        }
        error("could not find $relative from ${File(".").absolutePath}")
    }

    /** A banned word, matched on word boundaries so "must" does not fire on "mustard". */
    private data class Banned(val word: String) {
        private val pattern = Regex("""\b${Regex.escape(word)}\b""", RegexOption.IGNORE_CASE)
        fun matches(value: String) = pattern.containsMatchIn(value)
    }

    private companion object {
        const val SHOUT = 3

        /**
         * Names that are legitimately capitalised, and the only ones. COMPLIANCE.md
         * requires the questionnaire to be named and attributed exactly.
         */
        val PROPER_NAMES: Map<String, Set<String>> = mapOf(
            // COMPLIANCE.md requires the questionnaire to be named and attributed
            // exactly.
            "PAR-Q+" to setOf("readiness_link", "readiness_attribution"),
            // The names of two trials and a journal, in the one-line sources
            // CONTENT.md puts at the end of every card so the reader can look
            // them up. Changing their case would make them harder to find, which
            // is the opposite of why they are there.
            "STEP-1" to setOf("card_muscle_meds_source"),
            "SURMOUNT-1" to setOf("card_muscle_meds_source"),
            "JMIR" to setOf("card_smoothed_source"),
            // Three more of the same kind, from the cards ADDENDUM-03 Part 8
            // added: a recovery programme, a study group, and a cohort study.
            // Each is the name the reader would search for.
            "ERAS" to setOf("card_after_operation_source"),
            "PROT-AGE" to setOf("card_protein_source"),
            "PURE" to setOf("card_grip_source"),
            // Three licence names, on the About screen. COMPLIANCE.md and
            // PRIVACY.md name each of these exactly, and a licence written in any
            // other case is a different thing to look up. For the AGPL it would
            // also be a claim about a licence that does not exist.
            "AGPL-3.0" to setOf("about_licence"),
            "SIL" to setOf("about_made_with_body"),
            "MIT" to setOf("about_made_with_body"),
        )

        /**
         * The only places a banned word may appear, by word and by string.
         *
         * Rule 1 of the brief admits no exception for the app's own voice, and
         * none of these is the app's own voice. Two are the names of things:
         * a research method in a citation, and the questionnaire physical
         * therapists use by name. The third is a card whose whole subject is the
         * thing the app does not do, and a card called "Why there are no
         * calories here" cannot be written without the word. Banning it there
         * would leave the person without the explanation the ban exists to give
         * them.
         *
         * Every entry is pinned to the exact string that may use it, and the
         * test above fails if one of them stops needing it.
         */
        val ALLOWED_ANYWAY: Map<String, Set<String>> = mapOf(
            "calorie" to setOf("card_fasting_source", "card_no_calories_body"),
            "calories" to setOf("card_no_calories_title"),
            "patient" to setOf("card_what_you_want_source"),
        )
        const val TEN = 10

        /** DESIGN.md section 6, restated, with the words the capability frame added. */
        val BANNED = listOf(
            "rung", "tier", "trail", "trend", "postcard", "story", "check-in",
            "streak", "score", "goal", "target", "calories", "calorie", "burn",
            "earn", "cheat", "fail", "should", "must",
            "senior", "elderly", "frail", "frailty", "fall risk", "decline",
            "sarcopenia", "patient", "diagnosis", "prescribe",
            // The visit summary adds these, DESIGN.md section 6.
            "report", "assessment", "evaluation", "findings", "results",
            "recommendations",
        ).map { Banned(it) }
    }
}
