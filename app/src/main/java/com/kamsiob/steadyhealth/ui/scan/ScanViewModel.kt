package com.kamsiob.steadyhealth.ui.scan

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.data.DocumentRepository
import com.kamsiob.steadyhealth.data.PlanRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.entity.PlanItemEntity
import com.kamsiob.steadyhealth.plan.HowMany
import com.kamsiob.steadyhealth.plan.HowOften
import com.kamsiob.steadyhealth.plan.PlanItem
import com.kamsiob.steadyhealth.plan.PlanMatching
import com.kamsiob.steadyhealth.scan.PageKind
import com.kamsiob.steadyhealth.scan.PageText
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
class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db get() = SteadyDatabase.get(getApplication())
    private val documents get() = DocumentRepository(db)
    private val plans get() = PlanRepository(db)

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val _draft = MutableStateFlow(PlanDraftUiState())
    val draft: StateFlow<PlanDraftUiState> = _draft.asStateFlow()

    /** Set once the document is written, so the reading can find it again. */
    private var savedDocumentId: Long? = null

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
        return documents.save(
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
    }

    fun setPlanLabel(label: String) = _draft.update { it.copy(label = label) }

    /** Drop one proposed line. The person confirming is the point of the screen. */
    fun dropItem(at: Int) = _draft.update { state ->
        state.copy(items = state.items.filterIndexed { index, _ -> index != at })
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
        val planId = plans.save(label = draft.label.ifBlank { "" }, at = at)
        draft.items.forEach { plans.addItem(planId, row(planId, it, at)) }
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
    }
}
