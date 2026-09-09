package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * What still hangs off what, once it has all been put back.
 *
 * Every join in this database is by id, and the ids are handed out by the database
 * rather than chosen. That makes one particular failure both easy to write and
 * invisible afterwards: a restore that inserts the rows without their ids gets new
 * ones, the children keep the old numbers, and every count on every screen is
 * still right. A session with the right date and none of its movements. A plan
 * with no lines. A document with no photograph. Nothing says anything is wrong,
 * because nothing is missing, it is just no longer attached to what it was about.
 *
 * The fixture makes the ids awkward on purpose, so a renumbering cannot pass by
 * luck. The one session in the table has id three, the one document has id three,
 * the one plan has id three, the check has id five and the list items are nine and
 * eleven. A restore that renumbers gives every one of them id one.
 */
@RunWith(RobolectricTestRunner::class)
class RestoreJoinsTest {

    private val phone = aDatabase()
    private val newPhone = aDatabase()
    private lateinit var ids: Ids

    @After
    fun close() {
        phone.close()
        newPhone.close()
    }

    @Test
    fun aSessionKeepsItsMovements() = afterRestoring {
        assertThat(newPhone.runs().allOnce().map { it.id }).containsExactly(ids.run)
        val movements = newPhone.runs().movementsFor(ids.run)
        assertThat(movements.map { it.movementId }).containsExactly("sit_to_stand", "heel_raise")
        assertThat(newPhone.runs().allMovementsOnce()).hasSize(movements.size)
    }

    @Test
    fun aPlanKeepsItsLines() = afterRestoring {
        assertThat(newPhone.plans().all().map { it.id }).containsExactly(ids.plan)
        assertThat(newPhone.plans().itemsOf(ids.plan)).hasSize(2)
    }

    @Test
    fun aDocumentKeepsItsPages() = afterRestoring {
        assertThat(newPhone.documents().all().map { it.id }).containsExactly(ids.document)
        val pages = newPhone.documents().pagesOf(ids.document)
        assertThat(pages.map { it.at }).containsExactly(0, 1).inOrder()
        assertThat(pages.first().image.toList()).isEqualTo(listOf<Byte>(0, 1, -1, 127, -128))
    }

    @Test
    fun aCheckKeepsItsMeasures() = afterRestoring {
        assertThat(newPhone.checks().allOnce().map { it.id }).containsExactly(ids.check)
        val inTheCheck = newPhone.checks().allMeasuresOnce().filter { it.checkId == ids.check }
        assertThat(inTheCheck.map { it.measureId }).containsExactly("chair_stand", "walk_speed")
    }

    @Test
    fun aThingSomebodyWantsToDoKeepsItsRatings() = afterRestoring {
        assertThat(newPhone.abilities().allItemsOnce().map { it.id })
            .containsExactly(ids.item, ids.item + 2)
        val ratings = newPhone.abilities().ratingsFor(ids.item)
        assertThat(ratings.map { it.rating }).containsExactly(7, 0)
        assertThat(ratings.map { it.sureness }).containsExactly(4, null)
    }

    @Test
    fun aDayKeepsItsTags() = afterRestoring {
        val tags = newPhone.checkIns().tagsFor(ids.checkIn)
        assertThat(tags.map { it.tag }).containsExactly("sleep", "knees")
        assertThat(tags.first { it.tag == "sleep" }.fromReader).isTrue()
    }

    /**
     * Nothing points at a parent that is not there.
     *
     * The five joins above each name one thing. This is the same question asked of
     * every row at once, so a sixth relationship added later is covered without
     * anybody remembering to come back here.
     */
    @Test
    fun noRowPointsAtSomethingThatIsGone() = afterRestoring {
        val backup = BackupRepository(newPhone).read("0.1.0", LocalDate.of(2026, 9, 9))
        orphans(backup).forEach { (what, missing) ->
            assertWithMessage("$what points at rows that are not there").that(missing).isEmpty()
        }
    }

    private fun orphans(backup: Backup): Map<String, List<Long>> = mapOf(
        "run_movements.runId" to
            backup.runMovements.map { it.runId } - backup.runs.map { it.id }.toSet(),
        "plan_items.planId" to
            backup.planItems.map { it.planId } - backup.plans.map { it.id }.toSet(),
        "document_pages.documentId" to
            backup.documentPages.map { it.documentId } - backup.documents.map { it.id }.toSet(),
        "measure_results.checkId" to
            backup.measureResults.mapNotNull { it.checkId } - backup.checks.map { it.id }.toSet(),
        "item_ratings.itemId" to
            backup.itemRatings.map { it.itemId } - backup.trackedItems.map { it.id }.toSet(),
        "check_in_tags.checkInId" to
            backup.checkInTags.map { it.checkInId } - backup.checkIns.map { it.id }.toSet(),
    )

    private fun afterRestoring(check: suspend () -> Unit) = runTest {
        ids = phone.fillEveryTable()
        BackupRepository(newPhone).restore(
            BackupRepository(phone).read("0.1.0", LocalDate.of(2026, 9, 9)),
        )
        check()
    }
}
