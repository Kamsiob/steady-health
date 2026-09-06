package com.kamsiob.steadyhealth

import com.google.common.truth.Truth.assertThat
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

    private val strings: Map<String, String> by lazy { readStrings("strings.xml") }

    @Test
    fun noStringUsesAWordTheAppDoesNotHave() {
        val offences = strings.flatMap { (name, value) ->
            BANNED.filter { it.matches(value) }.map { "$name uses \"${it.word}\": $value" }
        }
        assertThat(offences).isEmpty()
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
        // DESIGN.md section 2: sentence case everywhere, no uppercase labels. A
        // string of three or more letters that is entirely upper case is a label
        // somebody shouted.
        val offences = strings.filterValues { value ->
            value.split(' ').any { word ->
                val letters = word.filter { it.isLetter() }
                letters.length >= SHOUT && letters == letters.uppercase()
            }
        }
        assertThat(offences.keys).isEmpty()
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
