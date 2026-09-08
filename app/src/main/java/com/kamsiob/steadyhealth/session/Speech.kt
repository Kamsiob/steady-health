package com.kamsiob.steadyhealth.session

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * The session's voice.
 *
 * ADDENDUM-03 Part 1: you cannot read a phone while standing up from a chair. Android
 * TextToSpeech, on device, no network, and the session runs identically in silence if
 * the engine is missing or the person turns it off.
 *
 * Three rules that are not preferences:
 * - It never speaks outside a session. There is no path into this class from anywhere
 *   but the session, and `stop` is called when the session ends however it ends.
 * - It never speaks a number the engine did not produce. Everything said here is
 *   passed in by the caller, and the caller reads it off the runner.
 * - It ducks other audio rather than stopping it. Somebody listening to the radio
 *   while they do their chair stands should still have the radio afterwards.
 */
class Speech(private val context: Context) {

    private var engine: TextToSpeech? = null
    private var ready = false
    private var focus: AudioFocusRequest? = null

    /** Below the system default, three steps, because the default is quick. */
    var rate: Int = DEFAULT_RATE
        set(value) {
            field = value.coerceIn(0, SLOWEST_STEPS - 1)
            engine?.setSpeechRate(rateFor(field))
        }

    var on: Boolean = true

    val available: Boolean get() = ready

    fun start(onReady: (Boolean) -> Unit = {}) {
        if (engine != null) {
            onReady(ready)
            return
        }
        engine = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                engine?.language = Locale.getDefault()
                engine?.setSpeechRate(rateFor(rate))
            }
            onReady(ready)
        }
    }

    /**
     * Say one line, interrupting whatever was being said.
     *
     * Interrupting is right for this: a rep count that queues behind a setup line is
     * a rep count that arrives after the rep.
     */
    fun say(line: String) {
        if (!on || !ready || line.isBlank()) return
        duck()
        engine?.speak(line, TextToSpeech.QUEUE_FLUSH, null, line.hashCode().toString())
    }

    /** Say one line after whatever is being said, for the pacing cues. */
    fun queue(line: String) {
        if (!on || !ready || line.isBlank()) return
        duck()
        engine?.speak(line, TextToSpeech.QUEUE_ADD, null, line.hashCode().toString())
    }

    fun stop() {
        engine?.stop()
        release()
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
        release()
    }

    /**
     * Ask the system to lower other audio rather than stop it.
     *
     * Requested once and held for the session, because asking per utterance makes the
     * radio jump up and down between every rep.
     */
    private fun duck() {
        if (focus != null) return
        val manager = context.getSystemService(AudioManager::class.java) ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .build()
        manager.requestAudioFocus(request)
        focus = request
    }

    private fun release() {
        val request = focus ?: return
        context.getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(request)
        focus = null
    }

    /** Unused today, kept because the pacing cues will need to know when one ends. */
    fun onDone(block: () -> Unit) {
        engine?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = block()

                @Deprecated("Required by the interface", ReplaceWith(""))
                override fun onError(utteranceId: String?) = Unit
            },
        )
    }

    private fun rateFor(step: Int): Float = SPEEDS[step.coerceIn(SPEEDS.indices)]

    companion object {
        /** Three steps, all below the system default of 1.0. */
        val SPEEDS = floatArrayOf(0.7f, 0.8f, 0.9f)
        const val SLOWEST_STEPS = 3
        const val DEFAULT_RATE = 1
    }
}
