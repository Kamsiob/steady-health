package com.kamsiob.steadyhealth.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat

/**
 * The theme, which is DESIGN.md and not Material's defaults.
 *
 * Material 3 is here for its components and its motion, not for its colour. Every
 * value below comes from the tokens in DESIGN.md section 2, and dynamic colour is
 * deliberately not wired up: an app whose whole promise is that nothing changes
 * colour because a number moved cannot also let the wallpaper repaint it.
 *
 * There is one theme. DESIGN.md says a dark one is not designed and not to invent
 * one, so [isSystemInDarkTheme] is read only to be ignored, in one place, with
 * this comment next to it, rather than being quietly absent.
 */
@Composable
fun SteadyTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE")
    val systemIsDark = isSystemInDarkTheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = SteadyColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

/**
 * Material's scheme, filled from the palette so that any component reaching for a
 * Material colour gets a Steady one rather than purple.
 */
private val SteadyColorScheme = lightColorScheme(
    primary = SteadyPalette.OrangeD,
    onPrimary = SteadyPalette.White,
    secondary = SteadyPalette.Navy,
    onSecondary = SteadyPalette.White,
    background = SteadyPalette.Ground,
    onBackground = SteadyPalette.Ink,
    surface = SteadyPalette.White,
    onSurface = SteadyPalette.Ink,
    surfaceVariant = SteadyPalette.Sand,
    onSurfaceVariant = SteadyPalette.Ink2,
    outline = SteadyPalette.Sand,
    // There is no red in this app, so the error role is navy on sand, which is
    // what DESIGN.md says a warning looks like here.
    error = SteadyPalette.Navy,
    onError = SteadyPalette.White,
    errorContainer = SteadyPalette.Sand,
    onErrorContainer = SteadyPalette.Navy,
)

/**
 * [Text] with a Steady role, so no screen has to remember to pass the family and
 * the weight. Every string in the app goes through one of these.
 */
@Composable
fun SteadyText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}

/** Kept so the unused-import check does not remove the context accessor. */
@Composable
internal fun currentPackageName(): String = LocalContext.current.packageName
