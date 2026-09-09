package com.kamsiob.steadyhealth.scan

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Reading the words off a photographed page, on this phone.
 *
 * ADDENDUM-03 Part 5. ML Kit's Latin text recognition runs entirely on the device:
 * the image is never uploaded and there is no network call in this file or under it.
 * That is the reason the scanning screen is allowed to say so at the moment somebody
 * points a camera at their own medical letter, which is the moment it matters.
 *
 * The recogniser is created once and closed when the screen is done with it. It holds
 * a native model; leaving one open per photograph would be a leak on a phone that has
 * already been described as not having much room.
 */
class PageText : AutoCloseable {

    private val recogniser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Every line of text on one page, joined in reading order.
     *
     * Returns an empty string rather than throwing when nothing could be read, because
     * a blurred photograph is an ordinary outcome and the screen above this one has a
     * good answer for it: take it again.
     */
    suspend fun of(bitmap: Bitmap): String = suspendCoroutine { waiting ->
        recogniser.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { waiting.resume(it.text) }
            .addOnFailureListener { waiting.resume("") }
    }

    override fun close() {
        recogniser.close()
    }
}
