package com.kamsiob.steadyhealth.ui

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.kamsiob.steadyhealth.R
import java.util.Locale

/**
 * Which languages this build actually speaks, and switching between them.
 *
 * The list is what has a translated `values-xx` folder in this build, not what
 * `locales_config.xml` hopes for. A picker offering a language the app then shows
 * in English is worse than no picker: somebody taps their own language, nothing
 * changes, and they conclude the app is broken rather than unfinished.
 *
 * So the picker appears only when there is a choice to make, and it disappears on
 * its own the moment a translation lands. Nothing else has to change for that to
 * happen.
 */
object Language {

    /** English is always here. It is the language the copy was written in. */
    private const val ENGLISH = "en"

    /** The order they are offered in, which is the order they were written in. */
    private val CANDIDATES = listOf(ENGLISH, "es", "zh", "ar")

    /**
     * The languages with real strings behind them.
     *
     * Checked by asking the resources for a string in that locale and seeing
     * whether it comes back different, which is the only question that matters
     * and the only one that stays right when a translation is added.
     */
    fun available(context: Context): List<String> {
        if (!switchable()) return listOf(ENGLISH)
        val english = string(context, ENGLISH)
        return CANDIDATES.filter { it == ENGLISH || string(context, it) != english }
    }

    /**
     * Switch the app's language, through the system's own per-app setting.
     *
     * Android 13 and up only, which is where per-app languages exist. Below it
     * the app follows the phone's language, which is the behaviour every app on
     * those versions has, and is stated here rather than papered over with a
     * dependency dragged in for one call.
     */
    fun set(context: Context, tag: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        context.getSystemService(LocaleManager::class.java)?.applicationLocales =
            LocaleList.forLanguageTags(tag)
    }

    /** True where [set] does anything, so a picker is not shown where it does not. */
    fun switchable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun current(context: Context): String {
        if (!switchable()) return context.resources.configuration.locales[0].language
        val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
        return locales?.takeUnless { it.isEmpty }?.get(0)?.language
            ?: context.resources.configuration.locales[0].language
    }

    /**
     * One string, read in one locale.
     *
     * `welcome_start` is the button on the first screen: short, always present,
     * and the first thing anybody would translate.
     */
    private fun string(context: Context, tag: String): String {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(tag))
        return context.createConfigurationContext(configuration).getString(R.string.welcome_start)
    }
}
