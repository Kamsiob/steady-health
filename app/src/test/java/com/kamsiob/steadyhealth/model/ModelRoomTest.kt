package com.kamsiob.steadyhealth.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * ADDENDUM-03 Part 7, "TWO MODELS, ONE CHOICE", as tests.
 *
 * Four rules are the ones worth holding, and the rest of this file is arithmetic
 * around them. The margin is never spent, whatever the sum comes to. A metered
 * connection never produces an answer a screen could act on without a tap. Removing
 * gives the work back to a manual path and reaches nothing else. And with neither
 * model on the phone there is no answer anywhere that says the app is short of
 * something, which is the one that would be quietly broken by a later refactor.
 */
class ModelRoomTest {

    @Test
    fun withNeitherModelTheAppIsWholeAndNothingSaysOtherwise() {
        val room = ModelRoom.of(freeBytes = 0, installed = emptySet(), connection = Connection.None)

        assertThat(room.nothingExtra).isTrue()
        assertWithMessage("every job is done some way with neither model on the phone")
            .that(room.byHand)
            .containsExactlyElementsIn(ModelJob.entries.map { it.manual }.toSet())
    }

    @Test
    fun bothModelsAreOfferedEvenOnAPhoneWithNoRoomForEither() {
        // A row saying how much more room it needs is more use than a row that is
        // not there, and Part 7 asks the screen to show each size.
        val room = ModelRoom.of(freeBytes = 0)

        assertThat(room.choices.map { it.model }).containsExactlyElementsIn(OptionalModel.entries).inOrder()
    }

    @Test
    fun theMarginIsNeverEatenIntoNoMatterHowCloseTheSumComes() {
        val model = OptionalModel.YourOwnWords
        val oneByteShort = ModelRoom.HEADROOM_BYTES + model.bytes - 1

        val answer = ModelRoom.canAdd(model, oneByteShort, emptySet(), Connection.Unmetered)

        assertThat(answer).isEqualTo(CanAdd.NotEnoughRoom(shortBy = 1))
    }

    @Test
    fun exactlyEnoughIsEnoughAndLeavesTheMarginStanding() {
        val model = OptionalModel.YourOwnWords
        val exactly = ModelRoom.HEADROOM_BYTES + model.bytes

        val answer = ModelRoom.canAdd(model, exactly, emptySet(), Connection.Unmetered)

        assertThat(answer).isEqualTo(CanAdd.Ready(roomLeftAfter = 0))
        assertWithMessage("the margin is still there afterwards")
            .that(exactly - model.bytes)
            .isEqualTo(ModelRoom.HEADROOM_BYTES)
    }

    @Test
    fun theShortfallSaysExactlyHowManyMoreBytesWouldDoIt() {
        val model = OptionalModel.DocumentsFromYourTherapist
        val short = ModelRoom.HEADROOM_BYTES + model.bytes - A_HUNDRED_MEGABYTES

        val answer = ModelRoom.canAdd(model, short, emptySet(), Connection.Unmetered)

        assertThat(answer).isEqualTo(CanAdd.NotEnoughRoom(shortBy = A_HUNDRED_MEGABYTES))
    }

    @Test
    fun onMobileDataTheAnswerIsAQuestionRatherThanAStart() {
        // "Neither downloads on a metered connection without an explicit tap." The
        // answer has to differ from Ready or a screen could treat the two the same.
        val answer = ModelRoom.canAdd(
            OptionalModel.YourOwnWords,
            PLENTY,
            emptySet(),
            Connection.Metered,
        )

        assertThat(answer).isInstanceOf(CanAdd.NeedsATap::class.java)
        assertThat(answer).isNotInstanceOf(CanAdd.Ready::class.java)
    }

    @Test
    fun onWifiTheAnswerIsReadyForEitherModel() {
        val room = ModelRoom.of(PLENTY, emptySet(), Connection.Unmetered)

        OptionalModel.entries.forEach {
            assertWithMessage(it.id).that(room.canAdd(it)).isInstanceOf(CanAdd.Ready::class.java)
        }
    }

    @Test
    fun withNothingToDownloadOverTheAnswerBlamesNobody() {
        val answer = ModelRoom.canAdd(OptionalModel.YourOwnWords, PLENTY, emptySet(), Connection.None)

        assertThat(answer).isEqualTo(CanAdd.NotConnected)
    }

    @Test
    fun beingShortOfRoomIsSaidBeforeAnythingAboutTheConnection() {
        // Short of space is the answer that is still true in an hour. Telling
        // somebody to wait for wi-fi when it would not fit either way wastes the wait.
        val answer = ModelRoom.canAdd(OptionalModel.YourOwnWords, 0, emptySet(), Connection.Metered)

        assertThat(answer).isInstanceOf(CanAdd.NotEnoughRoom::class.java)
    }

    @Test
    fun aModelAlreadyOnThePhoneIsNeverOfferedAgain() {
        val installed = setOf(OptionalModel.YourOwnWords)

        listOf(Connection.None, Connection.Metered, Connection.Unmetered).forEach { connection ->
            assertWithMessage(connection.id)
                .that(ModelRoom.canAdd(OptionalModel.YourOwnWords, 0, installed, connection))
                .isEqualTo(CanAdd.AlreadyHere)
        }
    }

    @Test
    fun theRoomLeftAfterOneIsWhatTheOtherHasToFitIn() {
        val forOneAndABit = ModelRoom.HEADROOM_BYTES + OptionalModel.YourOwnWords.bytes + A_HUNDRED_MEGABYTES

        assertThat(ModelRoom.bothFit(forOneAndABit)).isFalse()
        val answer = ModelRoom.canAdd(
            OptionalModel.DocumentsFromYourTherapist,
            forOneAndABit,
            setOf(OptionalModel.YourOwnWords),
            Connection.Unmetered,
        )
        // Free space has not moved in this call, because the phone reports it and the
        // engine does not guess at it. What has moved is that one is now installed.
        assertThat(answer).isInstanceOf(CanAdd.Ready::class.java)
    }

    @Test
    fun bothFitOnlyWhenBothSizesAndTheMarginAllFit() {
        val bothAndTheMargin = ModelRoom.HEADROOM_BYTES + OptionalModel.entries.sumOf { it.bytes }

        assertThat(ModelRoom.bothFit(bothAndTheMargin)).isTrue()
        assertThat(ModelRoom.bothFit(bothAndTheMargin - 1)).isFalse()
    }

    @Test
    fun whatIsAlreadyInstalledIsNotChargedForTwice() {
        val installed = setOf(OptionalModel.YourOwnWords)
        val both = OptionalModel.entries.toSet()

        assertThat(ModelRoom.needed(both, installed))
            .isEqualTo(OptionalModel.DocumentsFromYourTherapist.bytes)
        assertThat(ModelRoom.needed(both)).isEqualTo(both.sumOf { it.bytes })
    }

    @Test
    fun theRoomLeftIsNullRatherThanNegativeWhenItDoesNotFit() {
        val both = OptionalModel.entries.toSet()

        assertThat(ModelRoom.roomLeftAfter(both, freeBytes = 0, installed = emptySet())).isNull()
    }

    @Test
    fun aFullPhoneHasNoSpareBytesRatherThanFewerThanNone() {
        assertThat(ModelRoom.spare(0)).isEqualTo(0)
        assertThat(ModelRoom.spare(ModelRoom.HEADROOM_BYTES / 2)).isEqualTo(0)
        assertThat(ModelRoom.of(freeBytes = 0).spareBytes).isEqualTo(0)
    }

    @Test
    fun removingGivesTheWorkBackToTheManualPaths() {
        val removal = ModelRoom.remove(OptionalModel.YourOwnWords, OptionalModel.entries.toSet())

        assertThat(removal.installed).containsExactly(OptionalModel.DocumentsFromYourTherapist)
        assertThat(removal.nowByHand).containsExactly(
            ManualPath.TypeIt,
            ManualPath.PickTags,
            ManualPath.LookAtTheWeek,
        )
    }

    @Test
    fun removingTheOtherLeavesTheFirstAloneAndItsJobsAlone() {
        val removal = ModelRoom.remove(
            OptionalModel.DocumentsFromYourTherapist,
            OptionalModel.entries.toSet(),
        )

        assertThat(removal.installed).containsExactly(OptionalModel.YourOwnWords)
        assertThat(removal.nowByHand).containsExactly(ManualPath.LookAtThePhoto)
    }

    @Test
    fun removingSomethingThatIsNotThereSaysNothingAndChangesNothing() {
        val installed = setOf(OptionalModel.YourOwnWords)

        val removal = ModelRoom.remove(OptionalModel.DocumentsFromYourTherapist, installed)

        assertThat(removal.installed).isEqualTo(installed)
        assertWithMessage("the falling back line is only true when something fell back")
            .that(removal.nowByHand)
            .isEmpty()
    }

    @Test
    fun removingBothLeavesTheDefaultRatherThanAnEmptyState() {
        var installed = OptionalModel.entries.toSet()
        OptionalModel.entries.forEach { installed = ModelRoom.remove(it, installed).installed }

        assertThat(ModelRoom.of(PLENTY, installed, Connection.Unmetered).nothingExtra).isTrue()
    }

    @Test
    fun whatIsDoneByHandIsWhateverTheMissingModelWouldHaveDone() {
        val onlyWords = ModelRoom.of(PLENTY, setOf(OptionalModel.YourOwnWords), Connection.Unmetered)

        assertThat(onlyWords.byHand).containsExactly(ManualPath.LookAtThePhoto)
        assertThat(ModelRoom.of(PLENTY, OptionalModel.entries.toSet()).byHand).isEmpty()
    }

    @Test
    fun theTwoSizesAreTheOnesTheSpecificationNames() {
        // ADDENDUM-03 Part 7 says about 2.5 GB each and about five together, and says
        // to verify at build time. If a published size changes, this test is the note.
        OptionalModel.entries.forEach {
            assertWithMessage(it.weights)
                .that(it.bytes)
                .isEqualTo(OptionalModel.Sizes.ABOUT_TWO_AND_A_HALF_GB)
        }
        assertThat(OptionalModel.entries.sumOf { it.bytes }).isEqualTo(ABOUT_FIVE_GB)
    }

    @Test
    fun onlyTheMedicalWeightsCarryTermsToAcceptFirst() {
        assertThat(OptionalModel.DocumentsFromYourTherapist.licence.termsAcceptedBeforeDownload).isTrue()
        assertThat(OptionalModel.YourOwnWords.licence.termsAcceptedBeforeDownload).isFalse()
    }

    @Test
    fun everyJobBelongsToExactlyOneModel() {
        val claimed = OptionalModel.entries.flatMap { it.does }

        assertThat(claimed).containsExactlyElementsIn(ModelJob.entries)
    }

    private companion object {

        /** More room than any phone this app runs on, for the cases room is not about. */
        const val PLENTY = 64_000_000_000L

        const val A_HUNDRED_MEGABYTES = 100_000_000L

        const val ABOUT_FIVE_GB = 5_000_000_000L
    }
}
