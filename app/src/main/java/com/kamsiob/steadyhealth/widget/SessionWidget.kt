package com.kamsiob.steadyhealth.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kamsiob.steadyhealth.MainActivity
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.RunRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.session.SessionEngine
import com.kamsiob.steadyhealth.session.SessionInputs
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import java.time.LocalDate
import java.time.ZoneId

/** Set on the intent when the widget was tapped, so the app opens straight into it. */
const val START_SESSION = "start_session"

/** What the widget draws, read once when it is built. */
private data class WidgetSession(val length: String, val movements: String?)

/**
 * The widget. ADDENDUM-03 Part 13.
 *
 * Today's session, how long it takes, and a tap that starts it. Two sizes: the small
 * one is the length and the word Start, the wide one adds what is in it.
 *
 * It says what the card on Today says and is planned by the same call, because a
 * widget that promises a different session from the app is worse than no widget.
 */
class SessionWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val session = read(context)
        provideContent {
            val wide = LocalSize.current.width >= WIDE.width
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(navy)
                    .cornerRadius(CORNER)
                    .padding(PADDING)
                    .clickable(actionStartActivity(open(context))),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = context.getString(R.string.card_label),
                    style = TextStyle(color = sand, fontSize = SMALL_TEXT),
                )
                Text(
                    text = session.length,
                    style = TextStyle(color = white, fontSize = BIG_TEXT, fontWeight = FontWeight.Bold),
                )
                if (wide && session.movements != null) {
                    Text(
                        text = session.movements,
                        style = TextStyle(color = sand, fontSize = SMALL_TEXT),
                    )
                }
                Text(
                    text = context.getString(R.string.card_go),
                    style = TextStyle(color = orange, fontSize = SMALL_TEXT, fontWeight = FontWeight.Bold),
                )
            }
        }
    }

    /**
     * Today's session, planned exactly as the app plans it.
     *
     * The widget reads the encrypted database like anything else in the app does. It
     * shows the length and the movement names, which are the app's own words and not
     * anything about the person.
     */
    private suspend fun read(context: Context): WidgetSession {
        val db = SteadyDatabase.get(context)
        val profile = ProfileRepository(db)
        val runs = RunRepository(db)
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val plan = SessionEngine.plan(
            SessionInputs(
                way = profile.gettingAround(),
                exclusions = profile.exclusions(),
                kit = profile.kit(),
                sore = runs.soreAreas(today),
                history = runs.history(),
                lastFelt = runs.lastFelt(),
                feltBefore = runs.feltBefore(),
                today = today,
                lastSessionDay = runs.lastSessionDay(),
                strengthRunLength = runs.strengthRunLength(),
            ),
        )
        if (plan.steps.isEmpty()) {
            return WidgetSession(context.getString(R.string.card_nothing), movements = null)
        }
        return WidgetSession(
            length = context.resources.getQuantityString(
                R.plurals.card_minutes,
                plan.minutes,
                plan.minutes,
            ),
            movements = plan.main.joinToString(", ") { it.movement.name }.ifBlank { null },
        )
    }

    private fun open(context: Context): Intent =
        Intent(context, MainActivity::class.java).putExtra(START_SESSION, true)

    private companion object {
        val SMALL = DpSize(140.dp, 100.dp)
        val WIDE = DpSize(250.dp, 100.dp)
        val CORNER = 20.dp
        val PADDING = 12.dp
        val SMALL_TEXT = 13.sp
        val BIG_TEXT = 22.sp

        val navy = ColorProvider(SteadyPalette.Navy)
        val sand = ColorProvider(SteadyPalette.Sand)
        val white = ColorProvider(SteadyPalette.White)
        val orange = ColorProvider(SteadyPalette.Butter)
    }
}

/** The one receiver the manifest points at. */
class SessionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SessionWidget()
}
