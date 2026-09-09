package com.kamsiob.steadyhealth.ui.scan

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.DocumentRepository
import com.kamsiob.steadyhealth.data.PlanRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.entity.PlanItemEntity
import com.kamsiob.steadyhealth.plan.HowMany
import com.kamsiob.steadyhealth.plan.HowOften
import com.kamsiob.steadyhealth.plan.PlanItem
import com.kamsiob.steadyhealth.plan.PlanMatching
import com.kamsiob.steadyhealth.scan.PageKind
import com.kamsiob.steadyhealth.scan.PageText
import com.kamsiob.steadyhealth.ui.PlanConflicts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId

/** What the confirmation screen is working on. */
data class PlanDraftUiState(
    val items: List<PlanItem> = emptyList(),
    val label: String = "",
    /** The next appointment, as an epoch day. Optional, and stays optional. */
    val reviewDay: Long? = null,
    /**
     * Lines to raise at the appointment rather than to drop. ADDENDUM-03 Part 6.
     *
     * One sentence per plan movement that clashes with something the person said
     * they avoid. Nothing is removed and nothing is blocked by them.
     */
    val toAskAbout: List<String> = emptyList(),
    /**
     * Whether "This is your therapist's, not ours" still has to be said.
     *
     * Part 6 says that line once. False after the first plan is kept, so a second
     * plan does not repeat it.
     */
    val sayTheirs: Boolean = false,
)

/**
 * One scan, from the camera to a saved document and maybe a plan.
 *
 * ADDENDUM-03 Parts 5 and 6. The pages are held in memory while somebody is still
 * taking them, and written once when they say that is all of it, so a scan abandoned
 * halfway leaves nothing behind. The frames the camera produced for reading are never
 * written at all; only the pages kept.
 *
 * The classifier runs over the whole document rather than the first page, because a
 * covering letter with a sheet of exercises stapled behind it is one document and Part
 * 5's answer to it is "both offers, in that order".
 */
class ScanViewModel(private val application: Application) : AndroidViewModel(application) {

    private val db get() = SteadyDatabase.get(getApplication())
    private val documentsRepo get() = DocumentRepository(db)
    private val plans get() = PlanRepository(db)
    private val profile get() = ProfileRepository(db)

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val _draft = MutableStateFlow(PlanDraftUiState())
    val draft: StateFlow<PlanDraftUiState> = _draft.asStateFlow()

    /** Set once the document is written, so the reading can find it again. */
    private var savedDocumentId: Long? = null

    private val _documents = MutableStateFlow(DocumentsUiState())
    val documents: StateFlow<DocumentsUiState> = _documents.asStateFlow()

    fun open() {
        _state.value = ScanUiState(
            allowed = allowed(),
            hasCamera = getApplication<Application>().packageManager
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
        )
        savedDocumentId = null
    }

    fun allowedNow() = _state.update { it.copy(allowed = allowed()) }

    private fun allowed(): Boolean = ContextCompat.checkSelfPermission(
        getApplication(),
        android.Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * One page photographed and read.
     *
     * A page with no words on it is kept anyway, because somebody who photographed it
     * meant to, and the screen says nothing came off it rather than throwing it away.
     */
    fun took(image: Bitmap) = viewModelScope.launch {
        _state.update { it.copy(reading = true, nothingRead = false) }
        val text = PageText().use { it.of(image) }
        _state.update {
            it.copy(
                pages = it.pages + TakenPage(image, text),
                reading = false,
                nothingRead = text.isBlank(),
            )
        }
    }

    fun setWho(who: String) = _state.update { it.copy(fromWho = who) }

    /** Everything photographed, newest first. Part 5: viewable forever. */
    fun openDocuments() = viewModelScope.launch {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        _documents.value = DocumentsUiState(
            rows = documentsRepo.all().map { row ->
                DocumentRow(
                    id = row.id,
                    whenIt = whenSaid(today - row.epochDay),
                    fromWho = row.fromWho.ifBlank { null },
                )
            },
        )
    }

    /** Open one, which loads its pages. They are only decoded when somebody looks. */
    fun openDocument(id: Long) = viewModelScope.launch {
        if (_documents.value.openId == id) {
            _documents.update { it.copy(openId = null, pages = emptyList()) }
            return@launch
        }
        val pages = documentsRepo.pagesOf(id).mapNotNull {
            BitmapFactory.decodeByteArray(it.image, 0, it.image.size)
        }
        _documents.update { it.copy(openId = id, pages = pages) }
    }

    fun removeDocument(id: Long) = viewModelScope.launch {
        documentsRepo.remove(id)
        _documents.update { it.copy(openId = null, pages = emptyList()) }
        openDocuments()
    }

    /** "Today", "Yesterday", then a count of days. The same words the history uses. */
    private fun whenSaid(daysAgo: Long): String = when (daysAgo) {
        0L -> application.getString(R.string.history_today)
        1L -> application.getString(R.string.history_yesterday)
        else -> application.getString(R.string.history_days_ago, daysAgo.toInt())
    }

    /** Classify what was taken. Nothing is saved yet: the next screen offers that. */
    fun finished() {
        val text = _state.value.pages.joinToString("\n") { it.text }
        _state.update { it.copy(kind = PageKind.of(text)) }
    }

    /**
     * Keep the document. Always the outcome, whatever else was offered.
     *
     * Part 5: in every case the photo is saved, labelled by date and by who it came
     * from, and viewable forever beside anything the app produced from it.
     */
    fun keep(onDone: () -> Unit) = viewModelScope.launch {
        savedDocumentId = write()
        onDone()
    }

    private suspend fun write(): Long {
        val state = _state.value
        return documentsRepo.save(
            epochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay(),
            kind = state.kind?.let { it::class.simpleName }.orEmpty(),
            fromWho = state.fromWho,
            pages = state.pages.map { jpeg(it.image) to it.text },
            at = System.currentTimeMillis(),
        )
    }

    /**
     * Propose a plan from what was read, for the person to confirm.
     *
     * The document is written first, so that a plan somebody abandons on the
     * confirmation screen still leaves them the photograph they took.
     */
    fun proposePlan() = viewModelScope.launch {
        savedDocumentId = savedDocumentId ?: write()
        val text = _state.value.pages.joinToString("\n") { it.text }
        _draft.value = PlanDraftUiState(
            items = PlanMatching.readAll(text),
            label = _state.value.fromWho,
        )
        readTheDraftAgain()
    }

    /**
     * A plan proposed from one of the three ways in that have no page behind them.
     *
     * ADDENDUM-03 Part 6: saying it out loud, picking from the library and typing it
     * all end on the same confirmation screen as the photographed sheet, so they end
     * on the same draft rather than on a second one. Nothing is written here. The
     * items sit in memory until the person taps save on that screen, which is what
     * "nothing is saved unconfirmed" means for all four.
     *
     * No document is written either, because there is no photograph to keep: the
     * camera path saves the page first so that abandoning the plan still leaves the
     * picture, and there is nothing to leave here.
     */
    fun propose(items: List<PlanItem>) {
        _draft.value = PlanDraftUiState(items = items)
        viewModelScope.launch { readTheDraftAgain() }
    }

    /**
     * What the confirmation screen has to say about the draft as it now stands.
     *
     * Two things, and both are read from the person's own answers rather than from
     * the plan: which lines clash with something they said they avoid, and whether the
     * app still owes them the sentence about whose plan this is. Run again after a
     * line is taken out, because a clash the person has just removed is not a clash
     * any more and a sentence about it left on the screen would be about nothing.
     */
    private suspend fun readTheDraftAgain() {
        val lines = PlanConflicts.of(
            context = application,
            items = _draft.value.items,
            exclusions = profile.exclusions(),
            way = profile.gettingAround(),
        )
        val owed = !profile.seen(THEIRS_NOT_OURS)
        _draft.update { it.copy(toAskAbout = lines, sayTheirs = owed) }
    }

    /**
     * The person has read "This is your therapist's, not ours". ADDENDUM-03 Part 6.
     *
     * Part 6 asks for that line once, and the app was showing it on every plan
     * anybody ever confirmed. Once means once: the third sheet somebody photographs
     * does not need telling again who wrote it, and a sentence that keeps coming back
     * stops being a promise and becomes furniture.
     *
     * It is also marked read when a plan is saved, not only when the small dismiss is
     * tapped, because getting to a saved plan means passing it. Marking it only on the
     * dismiss would leave it there for everybody who read it and moved on, which is
     * most people, and that is the repetition this was meant to end.
     */
    fun theirsRead() = viewModelScope.launch { markTheirsRead() }

    private suspend fun markTheirsRead() {
        profile.markSeen(THEIRS_NOT_OURS)
        _draft.update { it.copy(sayTheirs = false) }
    }

    fun setPlanLabel(label: String) = _draft.update { it.copy(label = label) }

    fun setPlanReviewDay(day: Long?) = _draft.update { it.copy(reviewDay = day) }

    /** Drop one proposed line. The person confirming is the point of the screen. */
    fun dropItem(at: Int) = viewModelScope.launch {
        _draft.update { state ->
            state.copy(items = state.items.filterIndexed { index, _ -> index != at })
        }
        readTheDraftAgain()
    }

    /**
     * Keep the plan, exactly as confirmed.
     *
     * Every line is written with its own words and its own numbers. Nothing here
     * normalises, rounds, or fills anything in: an item the person left without a
     * number is stored without one, and the session screen asks at the time.
     */
    fun savePlan(onDone: () -> Unit) = viewModelScope.launch {
        val draft = _draft.value
        if (draft.items.isEmpty()) return@launch
        val at = System.currentTimeMillis()
        val planId = plans.save(
            // Trimmed, because "physio " and "physio" are the same therapist and the
            // card would print the first of them with a gap before the full stop. A
            // label of nothing but spaces is a label nobody filled in, and the card
            // falls back to the plain word for it.
            label = draft.label.trim(),
            at = at,
            reviewDay = draft.reviewDay,
        )
        draft.items.forEach { plans.addItem(planId, row(planId, it, at)) }
        markTheirsRead()
        _draft.value = PlanDraftUiState()
        onDone()
    }

    private fun row(planId: Long, item: PlanItem, at: Long) = PlanItemEntity(
        planId = planId,
        line = item.line,
        movementId = item.movement?.id,
        manyKind = manyKind(item.howMany),
        manyValue = manyValue(item.howMany),
        manySets = manySets(item.howMany),
        oftenKind = oftenKind(item.howOften),
        oftenTimes = oftenTimes(item.howOften),
        eachSide = item.eachSide,
        confirmedAt = at,
    )

    private fun manyKind(many: HowMany) = when (many) {
        HowMany.Unsaid -> "unsaid"
        is HowMany.Reps -> "reps"
        is HowMany.Hold -> "hold"
        is HowMany.Minutes -> "minutes"
    }

    private fun manyValue(many: HowMany) = when (many) {
        HowMany.Unsaid -> 0
        is HowMany.Reps -> many.reps
        is HowMany.Hold -> many.seconds
        is HowMany.Minutes -> many.minutes
    }

    private fun manySets(many: HowMany) = when (many) {
        is HowMany.Reps -> many.sets
        is HowMany.Hold -> many.sets
        else -> 1
    }

    private fun oftenKind(often: HowOften) = when (often) {
        HowOften.Unsaid -> "unsaid"
        is HowOften.ADay -> "day"
        is HowOften.AWeek -> "week"
        HowOften.EveryOtherDay -> "other_day"
    }

    private fun oftenTimes(often: HowOften) = when (often) {
        is HowOften.ADay -> often.times
        is HowOften.AWeek -> often.times
        else -> 0
    }

    /**
     * The page, as bytes to keep.
     *
     * JPEG at this quality is about a fifth of the size of the lossless version and a
     * page of typed text is still readable at it, which is what the photograph is for.
     */
    private fun jpeg(image: Bitmap): ByteArray = ByteArrayOutputStream().use { out ->
        image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        out.toByteArray()
    }

    private companion object {
        const val JPEG_QUALITY = 80

        /** The key Part 6's one line is remembered under, so it is said once ever. */
        const val THEIRS_NOT_OURS = "says_plan_theirs"
    }
}
