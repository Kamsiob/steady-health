package com.kamsiob.steadyhealth.export

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The export has to survive the round trip.
 *
 * The part of an export that is actually the person's is the sentences they
 * wrote, and a sentence can contain a comma, a quotation mark and a newline. An
 * export that mangles those is an export that quietly loses the only thing in the
 * file nobody else could reproduce.
 */
class DataExportTest {

    @Test
    fun anOrdinaryRowSurvives() {
        assertThat(DataExport.csv(listOf(listOf("day", "kg")))).isEqualTo("day,kg")
    }

    @Test
    fun everyAwkwardSentenceSurvivesTheRoundTrip() {
        awkward.forEach { sentence ->
            val written = DataExport.csv(listOf(listOf("2026-09-06", sentence, "7")))
            val read = DataExport.parse(written)
            assertWithMessage(sentence).that(read).hasSize(1)
            assertWithMessage(sentence).that(read.first()[1]).isEqualTo(sentence)
        }
    }

    @Test
    fun manyRowsComeBackAsManyRows() {
        val rows = listOf(
            listOf("day", "sentence"),
            listOf("1", "Knees ached, but I went anyway"),
            listOf("2", "Said \"not today\" and meant it"),
            listOf("3", "Two lines\nin one cell"),
        )
        assertThat(DataExport.parse(DataExport.csv(rows))).isEqualTo(rows)
    }

    @Test
    fun anEmptyExportIsAnEmptyList() {
        assertThat(DataExport.parse("")).isEmpty()
    }

    @Test
    fun aTrailingNewlineDoesNotMakeAnExtraRow() {
        assertThat(DataExport.parse("a,b\nc,d\n")).hasSize(2)
    }

    private val awkward = listOf(
        "Nothing much, quiet day",
        "Knees ached, then eased off",
        "Said \"I'll go tomorrow\" and did",
        "Two lines\nin one sentence",
        "Comma, quote \" and newline\ntogether",
        "Trailing comma,",
        ",Leading comma",
        "  spaces at both ends  ",
        "أمشي كل يوم",
        "走了二十分钟",
        "Caminé hasta la tienda",
    )
}
