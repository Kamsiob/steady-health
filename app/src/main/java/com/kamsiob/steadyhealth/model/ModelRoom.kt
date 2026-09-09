package com.kamsiob.steadyhealth.model

/**
 * How the phone is connected, as the decision layer needs to know it.
 *
 * Three values and not a boolean, because "not connected" and "connected but the
 * person pays for it" lead to different sentences and only one of them is a question
 * worth asking. This comes in as a parameter so the rules stay on the JVM; whatever
 * asks ConnectivityManager lives in the data layer.
 */
enum class Connection(val id: String) {

    /** Wi-fi, or anything the system does not consider metered. */
    Unmetered("unmetered"),

    /** Mobile data, or a hotspot. Two and a half gigabytes of somebody's allowance. */
    Metered("metered"),

    /** Nothing to download over. Not a refusal, and not the person's doing. */
    None("none"),
}

/**
 * Whether one model can be added right now, and what stands in the way when it cannot.
 *
 * Each of these becomes one sentence on the screen. None of them is an error and none
 * of them describes the phone or the person as lacking something: the phone either
 * has the room or it does not, and a metered connection is a question rather than a
 * problem.
 *
 * The bytes carried by [Ready] and [NeedsATap] are what would still be spendable
 * afterwards, with the margin already set aside. That is not the phone's free space
 * after the download, which is a gigabyte more, so a screen that printed this as
 * "left on your phone" would show somebody a nought that is not true.
 */
sealed interface CanAdd {

    /** It is already on the phone. Nothing to offer, and a remove option instead. */
    data object AlreadyHere : CanAdd

    /**
     * This build does not offer it yet, whatever the phone could hold.
     *
     * ADDENDUM-03 Part 7 under LICENSING: "A health tech attorney reviews this
     * boundary once before release; record it as BLOCKED until they have, and ship
     * the feature behind a flag that is off until it clears." Answered before
     * anything is said about room, because asking somebody to free up three gigabytes
     * for a download that cannot start is worse than saying nothing at all.
     */
    data object NotOfferedYet : CanAdd

    /**
     * There is room and the connection is free, so nothing measured here is in the way.
     *
     * Not the same as permission to start. [Licence.termsAcceptedBeforeDownload] may
     * still have terms to show first, and that gate is the screen's because only the
     * screen can show them and take the acceptance.
     */
    data class Ready(val roomLeftAfter: Long) : CanAdd

    /**
     * There is room, but this connection costs the person money.
     *
     * ADDENDUM-03 Part 7: "Neither downloads on a metered connection without an
     * explicit tap." This is that tap, and it is a separate answer from [Ready] so
     * that a screen cannot start one by treating every allowed case the same.
     */
    data class NeedsATap(val roomLeftAfter: Long) : CanAdd

    /** There is nothing to download over at the moment. Try again later, and no more. */
    data object NotConnected : CanAdd

    /**
     * How many more free bytes would make it possible, with the margin inside the number.
     *
     * The margin is counted in on purpose. If it were left out, a phone that is
     * already inside the margin would be told it is short by the size of the model
     * alone, and somebody who freed up exactly that much would be refused a second
     * time with a smaller number, which is the worst way to be told anything.
     */
    data class NotEnoughRoom(val shortBy: Long) : CanAdd
}

/**
 * The result of taking a model off the phone.
 *
 * ADDENDUM-03 Part 7: "Either can be removed at any time and everything already
 * produced stays. Removing one falls back to its manual path and says so once."
 * Nothing here refers to anything the model produced, which is deliberate: there is
 * no field in this type through which removing could reach a note, a tracked item or
 * a reading, so the promise that they stay is not something the caller has to
 * remember.
 */
data class Removal(
    val installed: Set<OptionalModel>,
    /** The paths that take the work back. Empty when nothing was actually removed. */
    val nowByHand: Set<ManualPath>,
)

/** One row on the screen: a model, and what may be done about it right now. */
data class Choice(val model: OptionalModel, val canAdd: CanAdd)

/**
 * Room on the phone for the optional models. ADDENDUM-03 Part 7, MASTER_SPEC.md 8.
 *
 * The engine behind "What the app can read". It answers three questions and no
 * others: what is on the phone, what could be added right now given the free bytes
 * and the connection, and how much room would be left afterwards. "The screen shows
 * each size and the phone's free space, and refuses gracefully rather than filling
 * the device", and refusing gracefully is arithmetic, so it belongs somewhere it can
 * be checked without a phone in a state nobody can reproduce.
 *
 * There is no state here called missing, incomplete, unavailable or not yet set up.
 * An empty [installed] is [nothingExtra], which is the default and a whole answer:
 * every job names a manual path, so the app with neither model is the app, not a
 * version of it waiting to be finished. That is the one thing in this file worth
 * protecting from a later refactor.
 *
 * Nothing here downloads, opens a socket, or touches a file, and that is deliberate.
 * The free bytes arrive as a number and the connection as a value, so every rule is
 * a pure function and every refusal has a test. The IO belongs in the data layer: a
 * small platform interface that reads free space from StatFs, watches the network
 * with ConnectivityManager, and runs the transfer under WorkManager, calling in here
 * before it starts and again when the numbers change. Residency in memory is a
 * different question again, owned by the memory manager described in
 * standards/kamsiob-project-template.md section C7, and it is not this file's.
 */
data class ModelRoom(
    val freeBytes: Long,
    val installed: Set<OptionalModel> = emptySet(),
    val connection: Connection = Connection.None,
    val offered: Set<OptionalModel> = OFFERED_NOW,
) {

    /**
     * True when neither model is on the phone.
     *
     * Named for what it is rather than for what it is not. The screen's default row
     * says the app works fully, and this is the flag that selects it.
     */
    val nothingExtra: Boolean get() = installed.isEmpty()

    /** Bytes that could still be spent, with the margin already taken off. */
    val spareBytes: Long get() = spare(freeBytes)

    /** What is being done by hand right now, for the one line under the default row. */
    val byHand: Set<ManualPath>
        get() = OptionalModel.entries.filterNot { it in installed }.flatMap { it.byHand }.toSet()

    /**
     * The whole screen: both models, in declaration order, whatever the phone can take.
     *
     * Worked out from the three numbers above rather than handed in, so there is no
     * way to hold a ModelRoom whose rows disagree with its own free space, and no way
     * to ask about a model the list happens not to contain. A row that says how much
     * more room it would need is more use than a row that is not there, and Part 7
     * asks the screen to show each size.
     */
    val choices: List<Choice> get() = OptionalModel.entries.map { Choice(it, canAdd(it)) }

    /** The answer for one model, without walking [choices]. */
    fun canAdd(model: OptionalModel): CanAdd =
        canAdd(model, freeBytes, installed, connection, offered)

    companion object {

        /**
         * Bytes that are never spent, whatever the arithmetic says. One gigabyte.
         *
         * A phone with nothing left stops taking photographs, stops installing its own
         * updates and starts showing the system's own storage warning, and a person
         * who let this app do that to their phone would be right to delete it. The
         * app's own store also keeps growing: every document photo is kept for good,
         * because Part 7 says the original is always kept and always viewable.
         *
         * A gigabyte is comfortably more than this app will ever hold on its own and
         * it is deliberately generous, because the two failures are not equal.
         * Refusing a download costs somebody one screen and a decision they can make
         * again tomorrow. Filling the device costs them the phone for an evening, and
         * this audience is the one least likely to know what to delete.
         */
        const val HEADROOM_BYTES = 1_000_000_000L

        /**
         * The models this build is allowed to offer at all, whatever the phone holds.
         *
         * ADDENDUM-03 Part 7 under LICENSING puts document reading behind a flag that
         * is off until a health tech attorney has reviewed the HAI-DEF boundary, so
         * until then the true answer about that download is that it is not on offer,
         * not that it would fit. The narrow set is the default on purpose: a caller
         * that has the clearance passes a wider one, and a caller that forgets to
         * pass anything cannot turn the feature on by omission.
         */
        val OFFERED_NOW: Set<OptionalModel> = setOf(OptionalModel.YourOwnWords)

        /**
         * Free bytes minus the margin, never below nought, for the screen's own line.
         *
         * Floored because a person does not have minus half a gigabyte of room, and
         * that is also why the refusal in [canAdd] does its own subtraction instead
         * of starting here.
         */
        fun spare(freeBytes: Long): Long =
            (freeBytes - HEADROOM_BYTES).coerceAtLeast(0)

        /** What a set of models costs, counting only the ones not already there. */
        fun needed(models: Set<OptionalModel>, installed: Set<OptionalModel> = emptySet()): Long =
            models.filterNot { it in installed }.sumOf { it.bytes }

        /**
         * Whether this model can be added right now.
         *
         * The order of the checks is the order of how permanent the answer is. What
         * the build offers at all comes before room, because a number to free up is
         * only worth printing next to something that could then be downloaded. Room
         * comes before the connection because being short of space is the answer that
         * is still true in an hour, and telling somebody on mobile data to wait for
         * wi-fi when the download would not fit either way wastes their evening.
         */
        fun canAdd(
            model: OptionalModel,
            freeBytes: Long,
            installed: Set<OptionalModel>,
            connection: Connection,
            offered: Set<OptionalModel> = OFFERED_NOW,
        ): CanAdd {
            if (model in installed) return CanAdd.AlreadyHere
            if (model !in offered) return CanAdd.NotOfferedYet
            // The margin comes off before any flooring, not after. Going through
            // spare() would report a phone that is already inside the margin as short
            // by the size of the model alone, which is less than it would actually
            // have to free, and the second refusal is the one nobody forgives.
            val left = freeBytes - HEADROOM_BYTES - model.bytes
            if (left < 0) return CanAdd.NotEnoughRoom(shortBy = -left)
            return when (connection) {
                Connection.None -> CanAdd.NotConnected
                Connection.Metered -> CanAdd.NeedsATap(roomLeftAfter = left)
                Connection.Unmetered -> CanAdd.Ready(roomLeftAfter = left)
            }
        }

        /**
         * Bytes still spendable after adding these, or null when they do not fit.
         *
         * Null rather than a negative number, so that a screen cannot render a
         * shortfall as though it were room. A phone already inside the margin holds
         * nothing more, so it answers null there too.
         */
        fun roomLeftAfter(models: Set<OptionalModel>, freeBytes: Long, installed: Set<OptionalModel>): Long? {
            val left = freeBytes - HEADROOM_BYTES - needed(models, installed)
            return if (left >= 0) left else null
        }

        /**
         * True when both would fit at once, which is the "Both" row in Part 7.
         *
         * Room only. Whether both are on offer is a different question, asked by
         * [canAdd], and running the two together would hide a licence behind a
         * storage answer.
         */
        fun bothFit(freeBytes: Long, installed: Set<OptionalModel> = emptySet()): Boolean =
            roomLeftAfter(OptionalModel.entries.toSet(), freeBytes, installed) != null

        /**
         * Take one off the phone.
         *
         * Removing a model that is not there is not an error and not a no-op worth
         * complaining about: it returns the same set and nothing to say, because the
         * sentence about falling back is only true when something actually fell back.
         */
        fun remove(model: OptionalModel, installed: Set<OptionalModel>): Removal =
            if (model in installed) {
                Removal(installed = installed - model, nowByHand = model.byHand)
            } else {
                Removal(installed = installed, nowByHand = emptySet())
            }
    }
}
