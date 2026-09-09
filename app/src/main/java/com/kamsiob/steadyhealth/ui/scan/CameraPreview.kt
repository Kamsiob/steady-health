@file:Suppress("MatchingDeclarationName") // The camera and the preview that shows it.

package com.kamsiob.steadyhealth.ui.scan

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * The camera, on screen, and one photograph taken from it.
 *
 * ADDENDUM-03 Part 5 and Part 7's storage rule: the frames the preview produces are
 * never written to disk. This class hands one bitmap back in memory and closes the
 * frame it came from immediately; the only thing that is ever written is the page
 * somebody chose to keep, and that is written by the view model.
 *
 * CameraX rather than the camera intent, because an intent hands the job to whatever
 * camera app is installed and the photograph would pass through that app's storage on
 * the way back. On device and out of anybody else's hands is the whole promise here.
 */
class Camera(private val context: Context) {

    private var capture: ImageCapture? = null

    /** Bind the preview and the still capture to a lifecycle, and hand back the view. */
    fun start(owner: LifecycleOwner, view: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = view.surfaceProvider
            }
            val still = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            capture = still
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    still,
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Take one photograph.
     *
     * [onTaken] gets a bitmap and nothing else. On any failure it is simply not
     * called: the screen above stays where it is with its button still there, which
     * is the right answer to a camera that did not manage it.
     */
    fun take(onTaken: (Bitmap) -> Unit) {
        val still = capture ?: return
        still.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    image.close()
                    onTaken(bitmap)
                }

                override fun onError(exception: ImageCaptureException) = Unit
            },
        )
    }
}

/**
 * The preview, sized so a sheet of A4 held at arm's length fills it.
 *
 * Taller than a square on purpose: every document this is pointed at is a portrait
 * page, and a preview that crops the bottom off one is a preview somebody has to
 * fight.
 */
@Composable
fun CameraPreview(camera: Camera, modifier: Modifier = Modifier) {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val view = remember { PreviewView(context) }

    AndroidView(
        factory = {
            camera.start(owner, view)
            view
        },
        modifier = modifier.fillMaxWidth().height(PREVIEW_HEIGHT),
    )
}

private val PREVIEW_HEIGHT = 420.dp
