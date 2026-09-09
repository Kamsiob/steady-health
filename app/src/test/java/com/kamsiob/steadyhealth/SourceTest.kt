package com.kamsiob.steadyhealth

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Two things about the source itself that no other test can see.
 *
 * Both are mistakes that compile, pass every test around them, and are wrong at
 * runtime in a way nobody notices until somebody's data is already stored under the
 * wrong key.
 */
class SourceTest {

    @Test
    fun noStringBuildsAKeyOutOfALiteralDollar() {
        // ${'$'} is the Kotlin way to write a dollar that does not interpolate, so
        // "place_${'$'}{it.id}" is the literal text place_${it.id} and every one of
        // six answers goes into the same setting. It has happened here twice, both
        // times from a script that escaped its own dollars on the way in, and both
        // times it compiled and passed.
        //
        // Nothing in this app has a reason to print a dollar. If one ever does, it
        // belongs in a string resource with the rest of the words.
        val offenders = kotlinFiles()
            .filter { it.readText().contains("\${'\$'}") }
            .map { it.name }

        assertWithMessage("a literal dollar in a string is almost always a mistake")
            .that(offenders)
            .isEmpty()
    }

    @Test
    fun nothingLogs() {
        // PRIVACY.md's claim is that health data has nowhere to go. Logcat is
        // somewhere, it is readable by anything with the right permission on some
        // builds, and a log line added to debug something has a way of shipping.
        val offenders = kotlinFiles()
            .filter { file ->
                file.readText().lineSequence().any { line ->
                    val code = line.substringBefore("//")
                    Regex("""\b(Log\.[dviwe]|println|printStackTrace)\b""").containsMatchIn(code)
                }
            }
            .map { it.name }

        assertWithMessage("nothing in this app writes to the log")
            .that(offenders)
            .isEmpty()
    }

    private fun kotlinFiles(): List<File> =
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()
}
