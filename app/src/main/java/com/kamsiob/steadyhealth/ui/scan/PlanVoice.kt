package com.kamsiob.steadyhealth.ui.scan

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * One spoken sentence turned into words, on this phone and nowhere else.
 *
 * ADDENDUM-03 Part 6's second way into a therapist's plan: "My physio wants me doing
 * ten sit to stands twice a day and heel raises". The words come back here and go
 * straight to the same reader the photographed sheet and the typed lines go to.
 *
 * ON DEVICE ONLY, AND THERE IS NO FALLBACK. Android's ordinary recogniser, the one
 * [SpeechRecognizer.createSpeechRecognizer] gives you, sends the audio to a server.
 * The app's standing rule is that everything runs locally and that the internet is
 * only ever touched by something the person switched on themselves, and a recording
 * of somebody describing their own therapy is exactly the thing that rule exists for.
 * So this uses [SpeechRecognizer.createOnDeviceSpeechRecognizer], which keeps the
 * audio on the phone, and when that is not available it says so and stops. The
 * fallback that would work is the one that sends somebody's voice to a server, so
 * there is no fallback: the screen offers typing instead, which reaches the same
 * confirmation screen by a path that asks for nothing.
 *
 * [available] is two questions and not one, and API 33 is the gate for both. The
 * recogniser can be created from API 31, but the question of whether the phone
 * actually has one, [SpeechRecognizer.isOnDeviceRecognitionAvailable], only arrived in
 * 33, and this app does not create a recogniser it cannot first ask about: a phone
 * with none hands back an object that answers every request with an error, which reads
 * to somebody standing there as the app not working. The app's floor is API 29, so a
 * phone below 33 is told the same plain thing as a phone with no recogniser installed.
 * Both are asked before the microphone permission is, because there is no reason to
 * ask for a microphone the app has already decided it cannot use.
 */
class PlanVoice(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    /** Whether this phone can turn speech into words without sending any of it away. */
    fun available(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    /**
     * Listen once, and hand back what was heard.
     *
     * [onHeard] gets the sentence. [onNothing] is every other ending, including a
     * refused microphone and a phone that heard silence, because the screen says the
     * same plain thing for all of them and a list of error numbers would be the app
     * explaining its own machinery to somebody holding a piece of paper.
     *
     * Must be called from the main thread, which is where the platform requires the
     * recogniser to be built and driven from.
     */
    fun listen(onHeard: (String) -> Unit, onNothing: () -> Unit) {
        // The version is asked again here rather than left to [available], which
        // already asked it. A tool reading this reads the branch it is standing in and
        // not a Boolean it was handed, and the call below is only legal inside it.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !available()) {
            onNothing()
            return
        }
        close()
        val listening = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = listening
        // The listener answers only while this recogniser is still the one in hand.
        // Letting go of one part way through a sentence produces a last callback on
        // some phones, and the screen would come back from being off saying nothing
        // came through, about a sentence nobody was there to say.
        listening.setRecognitionListener(
            Listener({ recognizer === listening }, onHeard, onNothing),
        )
        listening.startListening(request())
    }

    /** The person says that is the whole sentence. The result arrives as usual. */
    fun stop() {
        recognizer?.stopListening()
    }

    /**
     * Let go of the recogniser and the microphone with it.
     *
     * [stop] and this are not the same thing. [stop] is the person saying that is the
     * whole sentence, and the result still arrives. This is the screen going away,
     * where there is nobody to hand a sentence to, and a recogniser that is merely
     * stopped is one the phone still counts as held. Nothing is lost by it, because
     * [listen] builds a new one.
     */
    fun close() {
        recognizer?.destroy()
        recognizer = null
    }

    /**
     * What to listen for.
     *
     * Free form rather than a grammar, because a therapist's sentence is a therapist's
     * sentence and the reader downstream is the part that knows the vocabulary.
     * [RecognizerIntent.EXTRA_PREFER_OFFLINE] is set as well as the on-device
     * recogniser being asked for, so that a recogniser which ignores one of the two
     * still has the other saying the same thing.
     */
    private fun request(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    /**
     * The platform's callbacks, narrowed to the two answers the screen has.
     *
     * Partial results are deliberately not shown. A half heard sentence rewriting
     * itself on screen is the part of dictation people find unnerving, and this
     * screen has one job, which is to get one sentence to the confirmation screen.
     */
    private class Listener(
        private val live: () -> Boolean,
        private val onHeard: (String) -> Unit,
        private val onNothing: () -> Unit,
    ) : RecognitionListener {

        override fun onResults(results: Bundle?) {
            if (!live()) return
            val said = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (said.isBlank()) onNothing() else onHeard(said)
        }

        override fun onError(error: Int) {
            if (live()) onNothing()
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onPartialResults(partialResults: Bundle?) = Unit

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}
