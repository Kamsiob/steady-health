package com.kamsiob.steadyhealth.export

import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.entity.PlanItemEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One spreadsheet, and the tables it is made of.
 *
 * [from] is what makes "export everything" checkable rather than hopeful. It names
 * the tables this sheet carries, written down beside the sheet instead of left
 * implicit in its queries, so a test can hold every sheet against Room's own
 * description of the database and fail when a table has nowhere to appear.
 *
 * Without that, a table that arrives in a later phase is quietly left out of the
 * export and nobody finds out, because everything still works. The person who finds
 * out is the one who exported everything, believed the word, and then went looking
 * for the thing that is not there.
 */
class SheetOfTables(
    val name: String,
    val from: List<String>,
    val heading: List<String>,
    private val body: suspend (SteadyDatabase) -> List<List<String>>,
) {
    suspend fun of(db: SteadyDatabase): Sheet = Sheet(name, listOf(heading) + body(db))
}

/**
 * Every table, as spreadsheets somebody can open in anything.
 *
 * Two rules hold across all of them.
 *
 * They read through the backup's own queries rather than through the ones the
 * screens use. Several of those leave rows out on purpose, and every one of those
 * omissions is right for a screen and wrong here: `itemsOnce` hides an ability
 * somebody put away, `live` hides a plan they finished with, `soreOnce` hides an
 * area that has cleared. All three are things the person entered, and an export
 * that drops them is not an export of everything.
 *
 * They are lossy in the other direction, on purpose. Dates are formatted, the ids
 * that join one table to another are left out, and tags are flattened into a
 * column, because these are for a person to read. Nothing could be rebuilt from
 * them, which is why `backup.json` goes into the same zip.
 */
object EverySheet {

    /** Every sheet, in the order they are worth reading. */
    val all: List<SheetOfTables> =
        theBody() + saidAndDone() + checking() + writing() + paperwork() + held()

    suspend fun of(db: SteadyDatabase): List<Sheet> = all.map { it.of(db) }

    /** Weight, waist, blood pressure and the Sunday photographs. */
    private fun theBody(): List<SheetOfTables> = listOf(
        sheet(
            "weigh-ins",
            listOf("weigh_ins"),
            heading("date weight_kg smoothed_kg source"),
        ) { db ->
            db.backupHistory().weighIns().sortedBy { it.epochDay }.map {
                listOf(day(it.epochDay), it.rawKg.toString(), it.smoothedKg.toString(), it.source)
            }
        },
        sheet("waist", listOf("waist"), heading("date waist_cm")) { db ->
            db.backupHistory().waist().sortedBy { it.epochDay }.map {
                listOf(day(it.epochDay), it.centimetres.toString())
            }
        },
        sheet(
            "blood-pressure",
            listOf("blood_pressure"),
            heading("date systolic diastolic"),
        ) { db ->
            db.backupHistory().bloodPressure().sortedBy { it.epochDay }.map {
                listOf(day(it.epochDay), it.systolic.toString(), it.diastolic.toString())
            }
        },
        sheet("photos", listOf("photos"), heading("date picture_file")) { db ->
            db.backupHistory().photos().sortedBy { it.epochDay }.map {
                listOf(day(it.epochDay), it.fileName)
            }
        },
    )

    /** What somebody said about a day, and what they did on it. */
    private fun saidAndDone(): List<SheetOfTables> = listOf(
        sheet(
            "days",
            listOf("check_ins", "check_in_tags"),
            heading("date what_you_said sleep_hours how_it_went tags said_out_loud"),
        ) { db -> dayRows(db) },
        sheet(
            "sessions",
            listOf("sessions"),
            heading(
                "date kind step minutes could_you_talk steps distance_metres " +
                    "how_the_next_day_felt",
            ),
        ) { db ->
            db.backupHistory().sessions().sortedBy { it.epochDay }.map {
                listOf(
                    day(it.epochDay),
                    it.ladder,
                    it.stepIndex.toString(),
                    (it.durationSeconds / SECONDS_PER_MINUTE).toString(),
                    it.talkTest.orEmpty(),
                    it.steps?.toString().orEmpty(),
                    it.distanceMetres?.toString().orEmpty(),
                    it.nextDayFeel.orEmpty(),
                )
            }
        },
        sheet(
            "moving",
            listOf("runs", "run_movements"),
            heading(
                "date minutes how_it_ended how_it_felt ninety_seconds from_a_plan note " +
                    "movement how_many_asked_for how_many_done counted_by_you made_easier " +
                    "left_out",
            ),
        ) { db -> movingRows(db) },
        sheet(
            "sore",
            listOf("sore_areas"),
            heading("date area eased_on"),
        ) { db ->
            db.backupHistory().soreAreas().sortedBy { it.reportedOnDay }.map {
                listOf(
                    day(it.reportedOnDay),
                    it.area,
                    it.clearedOnDay?.let(::day).orEmpty(),
                )
            }
        },
    )

    /** The monthly check, the measures inside it, and the list it is measured against. */
    private fun checking(): List<SheetOfTables> = listOf(
        sheet(
            "monthly-checks",
            listOf("checks"),
            heading("date how_you_get_around"),
        ) { db ->
            db.backupHistory().checks().sortedBy { it.epochDay }.map {
                listOf(day(it.epochDay), it.gettingAround)
            }
        },
        sheet(
            "checks",
            listOf("measure_results"),
            heading("date measure ability value counted_by"),
        ) { db ->
            db.backupHistory().measureResults().sortedBy { it.epochDay }.map {
                listOf(
                    day(it.epochDay),
                    it.measureId,
                    it.domain,
                    it.value.toString(),
                    it.countedBy,
                )
            }
        },
        sheet(
            "your-list",
            listOf("tracked_items", "item_ratings"),
            heading("date what_you_wanted ability rating how_sure added_on put_away_on"),
        ) { db -> listRows(db) },
    )

    /** The Sunday write-up, what the app noticed, and the pages made for a visit. */
    private fun writing(): List<SheetOfTables> = listOf(
        sheet(
            "weekly-notes",
            listOf("weekly_notes"),
            heading("week_starting what_it_said written_by"),
        ) { db ->
            db.backupHistory().weeklyNotes().sortedBy { it.weekStartDay }.map {
                listOf(day(it.weekStartDay), it.paragraphs, writer(it.byModel))
            }
        },
        sheet(
            "noticing",
            listOf("patterns"),
            heading(
                "date_found tag measure direction weeks_with weeks_without split " +
                    "sentence detail",
            ),
        ) { db ->
            db.backupHistory().patterns().sortedBy { it.foundOnDay }.map {
                listOf(
                    day(it.foundOnDay),
                    it.tag,
                    it.measure,
                    it.direction,
                    it.weeksWith.toString(),
                    it.weeksWithout.toString(),
                    "${it.splitNumerator} of ${it.splitDenominator}",
                    it.sentence,
                    it.detail,
                )
            }
        },
        sheet(
            "try-it-and-see",
            listOf("experiments"),
            heading(
                "started_on switched_on ends_on stopped_on what_changed measure " +
                    "one_way other_way",
            ),
        ) { db ->
            db.backupHistory().experiments().sortedBy { it.startedOnDay }.map {
                listOf(
                    day(it.startedOnDay),
                    day(it.switchOnDay),
                    day(it.endsOnDay),
                    it.stoppedAt?.let(::dayFrom).orEmpty(),
                    it.variable,
                    it.measureId,
                    it.resultHeadline.orEmpty(),
                    it.resultDetail.orEmpty(),
                )
            }
        },
        sheet(
            "summaries",
            listOf("visit_summaries"),
            heading("made_on covering_from covering_to what_it_said questions by"),
        ) { db ->
            db.backupHistory().visitSummaries().sortedBy { it.generatedAt }.map {
                listOf(
                    dayFrom(it.generatedAt),
                    day(it.windowStartDay),
                    day(it.windowEndDay),
                    it.paragraphs,
                    it.questions,
                    writer(it.byModel),
                )
            }
        },
    )

    /** Photographed paperwork, and the programmes somebody was given. */
    private fun paperwork(): List<SheetOfTables> = listOf(
        sheet(
            "paperwork",
            listOf("documents", "document_pages"),
            heading("date what_it_is from_who page picture_file what_it_said"),
        ) { db -> paperworkRows(db) },
        sheet(
            "plans",
            listOf("plans", "plan_items"),
            heading(
                "plan given_on next_appointment put_away_on line movement how_many_kind " +
                    "how_many sets how_often_kind how_often each_side confirmed_on",
            ),
        ) { db -> planRows(db) },
    )

    /** The person's own words, where they are, and everything the app is holding. */
    private fun held(): List<SheetOfTables> = listOf(
        sheet(
            "your-words",
            listOf("person_synonyms"),
            heading("phrase what_you_meant learned_on"),
        ) { db ->
            db.backupState().synonyms().sortedBy { it.learnedAt }.map {
                listOf(it.phrase, it.tag, dayFrom(it.learnedAt))
            }
        },
        sheet(
            "step-names",
            listOf("step_names"),
            heading("kind step your_name named_on"),
        ) { db ->
            db.backupState().stepNames().map {
                listOf(it.ladder, it.stepIndex.toString(), it.name, dayFrom(it.namedAt))
            }
        },
        sheet(
            "where-you-are",
            listOf("ladder_state"),
            heading("kind step last_offered declined_until easing_until last_time"),
        ) { db ->
            db.backupState().ladderState().map {
                listOf(
                    it.ladder,
                    it.currentStepIndex.toString(),
                    it.lastOfferedDay?.let(::day).orEmpty(),
                    it.offerDeclinedUntilDay?.let(::day).orEmpty(),
                    it.easingUntilDay?.let(::day).orEmpty(),
                    it.lastSessionDay?.let(::day).orEmpty(),
                )
            }
        },
        sheet("settings", listOf("settings"), heading("name value")) { db ->
            db.backupState().settings().sortedBy { it.key }.map { listOf(it.key, it.value) }
        },
        sheet("left-out", listOf("exclusions"), heading("left_out chosen_on")) { db ->
            db.backupState().exclusions().map { listOf(it.exclusion, dayFrom(it.chosenAt)) }
        },
        sheet(
            "before-you-start",
            listOf("readiness"),
            heading("question you_said_yes answered_on"),
        ) { db ->
            db.backupState().readiness().map {
                listOf(it.flag, it.answeredYes.toString(), dayFrom(it.answeredAt))
            }
        },
        sheet(
            "what-the-app-did",
            listOf("notices", "reminders_sent", "daily_prompts"),
            heading("date what detail"),
        ) { db -> appRows(db) },
    )

    private suspend fun dayRows(db: SteadyDatabase): List<List<String>> {
        val tags = db.backupHistory().checkInTags().groupBy { it.checkInId }
        return db.backupHistory().checkIns().sortedBy { it.epochDay }.map { said ->
            listOf(
                day(said.epochDay),
                said.sentence,
                said.sleepHalfHours?.let { (it / 2.0).toString() }.orEmpty(),
                said.dayRating.orEmpty(),
                tags[said.id].orEmpty().joinToString(" ") { it.tag },
                said.spoken.toString(),
            )
        }
    }

    /**
     * A session and what was done in it, one row per movement.
     *
     * A session with no movements still gets a row. It happened, somebody started
     * it, and a session that vanishes from the export because it was stopped before
     * the first movement is a day of theirs the export does not admit to.
     */
    private suspend fun movingRows(db: SteadyDatabase): List<List<String>> {
        val movements = db.backupHistory().runMovements().groupBy { it.runId }
        return db.backupHistory().runs().sortedBy { it.epochDay }.flatMap { run ->
            val head = listOf(
                day(run.epochDay),
                ((run.endedAt - run.startedAt) / MILLIS_PER_MINUTE).toString(),
                run.ending,
                run.felt.orEmpty(),
                run.small.toString(),
                run.fromPlan.toString(),
                run.note,
            )
            val mine = movements[run.id].orEmpty()
            if (mine.isEmpty()) {
                listOf(head + List(MOVEMENT_COLUMNS) { "" })
            } else {
                mine.map { movement ->
                    head + listOf(
                        movement.movementId,
                        movement.target.toString(),
                        movement.count.toString(),
                        movement.selfReported.toString(),
                        movement.madeEasier.toString(),
                        movement.skipped.toString(),
                    )
                }
            }
        }
    }

    /**
     * Everything on the list with every rating of it, put-away items included.
     *
     * An item nobody has rated yet still gets a row, because the sentence they wrote
     * is the part of this that is theirs and it exists whether or not a number does.
     */
    private suspend fun listRows(db: SteadyDatabase): List<List<String>> {
        val ratings = db.backupHistory().itemRatings().groupBy { it.itemId }
        return db.backupState().trackedItems().sortedBy { it.createdAt }.flatMap { item ->
            val tail = listOf(dayFrom(item.createdAt), item.archivedAt?.let(::dayFrom).orEmpty())
            val mine = ratings[item.id].orEmpty().sortedBy { it.epochDay }
            if (mine.isEmpty()) {
                listOf(listOf("", item.text, item.domain, "", "") + tail)
            } else {
                mine.map { rating ->
                    listOf(
                        day(rating.epochDay),
                        item.text,
                        item.domain,
                        rating.rating.toString(),
                        rating.sureness?.toString().orEmpty(),
                    ) + tail
                }
            }
        }
    }

    /**
     * One row per photographed page, naming the picture that is in the same zip.
     *
     * `picture_file` is the name [Pictures] writes the photograph under, so the
     * spreadsheet and the photograph beside it can be read together. A document with
     * no page still gets a row rather than disappearing.
     */
    private suspend fun paperworkRows(db: SteadyDatabase): List<List<String>> {
        val pages = db.backupState().documentPages().groupBy { it.documentId }
        return db.backupState().documents().sortedBy { it.epochDay }.flatMap { paper ->
            val head = listOf(day(paper.epochDay), paper.kind, paper.fromWho)
            val mine = pages[paper.id].orEmpty().sortedBy { it.at }
            if (mine.isEmpty()) {
                listOf(head + List(PAGE_COLUMNS) { "" })
            } else {
                mine.map { page ->
                    head + listOf(
                        (page.at + 1).toString(),
                        Pictures.nameOf(paper.id, page.at),
                        page.text,
                    )
                }
            }
        }
    }

    /** One row per line of a plan, and one row for a plan that has no lines yet. */
    private suspend fun planRows(db: SteadyDatabase): List<List<String>> {
        val lines = db.backupState().planItems().groupBy { it.planId }
        return db.backupState().plans().sortedBy { it.createdAt }.flatMap { plan ->
            val head = listOf(
                plan.label,
                dayFrom(plan.createdAt),
                plan.reviewDay?.let(::day).orEmpty(),
                plan.archivedAt?.let(::dayFrom).orEmpty(),
            )
            val mine = lines[plan.id].orEmpty()
            if (mine.isEmpty()) {
                listOf(head + List(LINE_COLUMNS) { "" })
            } else {
                mine.map { head + line(it) }
            }
        }
    }

    private fun line(item: PlanItemEntity): List<String> =
        listOf(
            item.line,
            item.movementId.orEmpty(),
            item.manyKind,
            item.manyValue.toString(),
            item.manySets.toString(),
            item.oftenKind,
            item.oftenTimes.toString(),
            item.eachSide.toString(),
            dayFrom(item.confirmedAt),
        )

    /**
     * What the app said and when, which is the app's side of the record.
     *
     * Three small tables in one sheet because they answer one question between
     * them, and three files of two columns each would be three files nobody opens.
     */
    private suspend fun appRows(db: SteadyDatabase): List<List<String>> {
        val shown = db.backupState().notices().map {
            listOf(dayFrom(it.shownAt), "note shown", it.noticeId)
        }
        val sent = db.backupHistory().remindersSent().map {
            listOf(dayFrom(it.sentAt), "reminder sent", it.type)
        }
        val asked = db.backupHistory().dailyPrompts().map {
            listOf(day(it.epochDay), "daily question sent", if (it.opened) "opened" else "")
        }
        return (shown + sent + asked).sortedBy { it.first() }
    }

    private fun sheet(
        name: String,
        from: List<String>,
        heading: List<String>,
        body: suspend (SteadyDatabase) -> List<List<String>>,
    ) = SheetOfTables(name, from, heading, body)

    /**
     * A heading, written as one line of column names.
     *
     * Twenty-four headings written as lists of quoted strings is twenty-four
     * headings nobody reads, wrapped one name per line. Written as a sentence, the
     * order of the columns is visible at a glance, which is the only thing about a
     * heading anybody ever needs to check.
     */
    private fun heading(names: String): List<String> = names.split(" ")

    private fun day(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).toString()

    private fun dayFrom(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    /** Said plainly, because "byModel" is not a word anybody outside this app knows. */
    private fun writer(byModel: Boolean): String = if (byModel) "the reader" else "the app"

    private const val SECONDS_PER_MINUTE = 60
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MOVEMENT_COLUMNS = 6
    private const val PAGE_COLUMNS = 3
    private const val LINE_COLUMNS = 9
}
