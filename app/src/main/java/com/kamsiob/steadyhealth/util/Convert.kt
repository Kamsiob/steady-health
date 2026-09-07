package com.kamsiob.steadyhealth.util

import com.kamsiob.steadyhealth.domain.Units
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Kilos and centimetres are what the app stores; pounds and feet are what a lot of
 * people think in.
 *
 * Stored metric always, converted at the edge. The alternative, storing whatever
 * unit somebody happened to pick, means every calculation has to ask first and one
 * of them eventually forgets.
 */
object Convert {

    private const val LB_PER_KG = 2.2046226218
    private const val CM_PER_INCH = 2.54
    private const val INCHES_PER_FOOT = 12

    fun kgToLb(kg: Double): Double = kg * LB_PER_KG

    fun lbToKg(lb: Double): Double = lb / LB_PER_KG

    /** The weight as the person reads it, to one decimal place. */
    fun weightLabel(kg: Double, units: Units): String {
        val value = if (units == Units.Imperial) kgToLb(kg) else kg
        return String.format(Locale.US, "%.1f", value)
    }

    /** The height as the person reads it: 5 ft 7, or 170 cm. */
    fun heightLabel(cm: Double, units: Units): String = if (units == Units.Metric) {
        "${cm.roundToInt()} cm"
    } else {
        val totalInches = (cm / CM_PER_INCH).roundToInt()
        "${totalInches / INCHES_PER_FOOT} ft ${totalInches % INCHES_PER_FOOT}"
    }
}
