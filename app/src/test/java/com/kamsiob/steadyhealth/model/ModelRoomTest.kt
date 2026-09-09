package com.kamsiob.steadyhealth.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * ADDENDUM-03 Part 7, "TWO MODELS, ONE CHOICE", as tests.
 *
 * Five rules are the ones worth holding, and the rest of this file is arithmetic
 * around them. The margin is never spent, and the number the app asks somebody to
 * free up is the number that actually works, counted from where their phone is now.
 * A metered connection never produces an answer a screen could act on without a tap.
 * A model the build has not been cleared to offer is not offered, whatever the phone
 * could hold. Removing gives the work back to a manual path and reaches nothing else.
 * And with neither model on the phone there is no answer anywhere that says the app
 * is short of something, which is the one that would be quietly broken by a later
 * refactor.
 */
class ModelRoomTest {

    @Test
    fun withNeitherModelTheAppIsWholeAndNothingSaysOtherwise() {
        val room = ModelRoom(freeBytes = 0, installed = emptySet(), connection = Connection.None)

        assertThat(room.nothingExtra).isTrue()
        assertWithMessage("every job is done some way with neither model on the phone")
            .that(room.byHand)
            .containsExactlyElementsIn(ModelJob.entries.map { it.manual }.toSet())
    }

    @Test
    fun bothModelsGetARowEvenOnAPhoneWithNoRoomForEither() {
        // A row saying how much more room it needs is more use than a row that is
        // not there, and Part 7 asks the screen to show each size.
        val room = ModelRoom(freeBytes = 0)

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

        val answer = ModelRoom.canAdd(model, short, emptySet(), Connection.Unmetered, BOTH_OFFERED)

        assertThat(answer).isEqualTo(CanAdd.NotEnoughRoom(shortBy = A_HUNDRED_MEGABYTES))
    }

    @Test
    fun theShortfallCountsTheMarginEvenWhenThePhoneIsAlreadyInsideIt() {
        // The failure this stops: a phone with less free space than the margin being
        // told it is short by the size of the model, freeing exactly that, and being
        // refused a second time with a smaller number.
        val model = OptionalModel.YourOwnWords
        val nearlyFull = HALF_A_GIGABYTE

        val answer = ModelRoom.canAdd(model, nearlyFull, emptySet(), Connection.Unmetered)

        val shortBy = (answer as CanAdd.NotEnoughRoom).shortBy
        assertWithMessage("the margin is part of what has to be freed")
            .that(shortBy)
            .isEqualTo(ModelRoom.HEADROOM_BYTES + model.bytes - nearlyFull)
        assertWithMessage("freeing exactly what was asked for is enough the second time")
            .that(ModelRoom.canAdd(model, nearlyFull + shortBy, emptySet(), Connection.Unmetered))
            .isEqualTo(CanAdd.Ready(roomLeftAfter = 0))
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
    fun onWifiTheAnswerIsReadyForEitherModelOnceBothAreOnOffer() {
        val room = ModelRoom(PLENTY, emptySet(), Connection.Unmetered, BOTH_OFFERED)

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
    fun theReadingModelIsNotOnOfferUntilTheBuildSaysItIs() {
        // Part 7: the feature ships behind a flag that is off until a health tech
        // attorney has reviewed the HAI-DEF boundary. The default has to be the off one.
        assertThat(ModelRoom.OFFERED_NOW).doesNotContain(OptionalModel.DocumentsFromYourTherapist)

        val answer = ModelRoom.canAdd(
            OptionalModel.DocumentsFromYourTherapist,
            PLENTY,
            emptySet(),
            Connection.Unmetered,
        )

        assertThat(answer).isEqualTo(CanAdd.NotOfferedYet)
    }

    @Test
    fun whatIsNotOnOfferIsSaidBeforeAnythingAboutRoom() {
        // Asking somebody to free up three gigabytes for a download that cannot start
        // is worse than saying nothing about the space at all.
        val answer = ModelRoom.canAdd(
            OptionalModel.DocumentsFromYourTherapist,
            0,
            emptySet(),
            Connection.Unmetered,
        )

        assertThat(answer).isEqualTo(CanAdd.NotOfferedYet)
    }

    @Test
    fun aModelOnThePhoneStaysOnThePhoneEvenIfTheBuildStopsOfferingIt() {
        val installed = setOf(OptionalModel.DocumentsFromYourTherapist)

        val answer = ModelRoom.canAdd(
            OptionalModel.DocumentsFromYourTherapist,
            PLENTY,
            installed,
            Connection.Unmetered,
        )

        assertThat(answer).isEqualTo(CanAdd.AlreadyHere)
    }

    @Test
    fun eachRowAnswersForItsOwnModelAndNotForTheOther() {
        val room = ModelRoom(PLENTY, setOf(OptionalModel.YourOwnWords), Connection.Unmetered)

        assertThat(room.canAdd(OptionalModel.YourOwnWords)).isEqualTo(CanAdd.AlreadyHere)
        assertThat(room.canAdd(OptionalModel.DocumentsFromYourTherapist)).isEqualTo(CanAdd.NotOfferedYet)
        room.choices.forEach {
            assertWithMessage(it.model.id).that(it.canAdd).isEqualTo(room.canAdd(it.model))
        }
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
    fun theRoomLeftIsWhatIsStillSpendableAndNotWhatThePhoneWillShow() {
        val model = OptionalModel.YourOwnWords
        val free = ModelRoom.HEADROOM_BYTES + model.bytes + A_HUNDRED_MEGABYTES

        val answer = ModelRoom.canAdd(model, free, emptySet(), Connection.Unmetered) as CanAdd.Ready

        assertThat(answer.roomLeftAfter).isEqualTo(A_HUNDRED_MEGABYTES)
        assertWithMessage("the phone itself keeps the margin on top of that")
            .that(free - model.bytes)
            .isEqualTo(answer.roomLeftAfter + ModelRoom.HEADROOM_BYTES)
    }

    @Test
    fun theRoomLeftAfterOneIsWhatTheOtherHasToFitIn() {
        val both = OptionalModel.entries.sumOf { it.bytes }
        val first = OptionalModel.YourOwnWords
        val forBothAndABit = ModelRoom.HEADROOM_BYTES + both + A_HUNDRED_MEGABYTES

        assertThat(ModelRoom.bothFit(forBothAndABit)).isTrue()
        // Installing the first one is the phone losing that many bytes, which is the
        // number the second download now has to fit inside.
        val afterTheFirst = forBothAndABit - first.bytes
        val answer = ModelRoom.canAdd(
            OptionalModel.DocumentsFromYourTherapist,
            afterTheFirst,
            setOf(first),
            Connection.Unmetered,
            BOTH_OFFERED,
        )

        assertThat(answer).isEqualTo(CanAdd.Ready(roomLeftAfter = A_HUNDRED_MEGABYTES))
    }

    @Test
    fun theSecondModelIsRefusedWhenTheFirstOneUsedUpTheRoom() {
        val first = OptionalModel.YourOwnWords
        val forOneAndABit = ModelRoom.HEADROOM_BYTES + first.bytes + A_HUNDRED_MEGABYTES

        assertThat(ModelRoom.bothFit(forOneAndABit)).isFalse()
        val answer = ModelRoom.canAdd(
            OptionalModel.DocumentsFromYourTherapist,
            forOneAndABit - first.bytes,
            setOf(first),
            Connection.Unmetered,
            BOTH_OFFERED,
        )

        assertThat(answer).isEqualTo(
            CanAdd.NotEnoughRoom(
                shortBy = OptionalModel.DocumentsFromYourTherapist.bytes - A_HUNDRED_MEGABYTES,
            ),
        )
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
        assertThat(ModelRoom(freeBytes = 0).spareBytes).isEqualTo(0)
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

        assertThat(ModelRoom(PLENTY, installed, Connection.Unmetered).nothingExtra).isTrue()
    }

    @Test
    fun whatIsDoneByHandIsWhateverTheMissingModelWouldHaveDone() {
        val onlyWords = ModelRoom(PLENTY, setOf(OptionalModel.YourOwnWords), Connection.Unmetered)

        assertThat(onlyWords.byHand).containsExactly(ManualPath.LookAtThePhoto)
        assertThat(ModelRoom(PLENTY, OptionalModel.entries.toSet()).byHand).isEmpty()
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
    fun aReadyAnswerNeverStandsInForTermsNobodyHasAccepted() {
        // The engine answers about room and the connection and nothing else, so a
        // screen holding Ready for these weights still has HAI-DEF terms to show.
        val model = OptionalModel.DocumentsFromYourTherapist
        val room = ModelRoom(PLENTY, emptySet(), Connection.Unmetered, BOTH_OFFERED)

        assertThat(room.canAdd(model)).isInstanceOf(CanAdd.Ready::class.java)
        assertWithMessage("the licence, not the answer, is what says a tap is still owed")
            .that(model.licence.termsAcceptedBeforeDownload)
            .isTrue()
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

        /** Less free space than the margin, which is where the arithmetic used to slip. */
        const val HALF_A_GIGABYTE = 500_000_000L

        const val ABOUT_FIVE_GB = 5_000_000_000L

        /** The set a build passes once the HAI-DEF boundary has been reviewed. */
        val BOTH_OFFERED: Set<OptionalModel> = OptionalModel.entries.toSet()
    }
}
