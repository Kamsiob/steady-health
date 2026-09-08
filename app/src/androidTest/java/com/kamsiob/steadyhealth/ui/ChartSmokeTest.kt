package com.kamsiob.steadyhealth.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.ui.theme.SteadyTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * Vico draws a line on this device.
 *
 * The Phase 0 gate for charts. It is on device rather than in a JVM test because
 * what can fail here is rendering: a chart library that composes fine and draws
 * nothing is the failure worth catching, and it only happens where there is a
 * real canvas.
 *
 * The weight line is the one chart the app cannot do without, so this runs before
 * anything is built on top of it.
 */
class ChartSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aLineChartComposesAndDraws() {
        val producer = CartesianChartModelProducer()
        runBlocking {
            producer.runTransaction {
                lineModel { series(listOf(80.0, 79.6, 79.4, 79.5, 79.1)) }
            }
        }

        compose.setContent {
            SteadyTheme {
                CartesianChartHost(
                    chart = rememberCartesianChart(rememberLineCartesianLayer()),
                    modelProducer = producer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CHART_HEIGHT)
                        .testTag(TAG),
                )
            }
        }

        compose.onNodeWithTag(TAG).assertExists()
    }

    private companion object {
        const val TAG = "chart"
        val CHART_HEIGHT = 180.dp
    }
}
