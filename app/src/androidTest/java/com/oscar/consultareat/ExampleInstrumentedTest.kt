package com.oscar.consultareat

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:org.junit.Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.oscar.consultareat", appContext.packageName)
    }

    @Test
    fun botonInicioAbreLaPantallaPrincipalDesdeLasPantallasInternas() {
        navegarYVolverAInicio("Consultas")
        navegarYVolverAInicio("Historial")
        navegarYVolverAInicio("Acerca de")
    }

    private fun navegarYVolverAInicio(titulo: String) {
        composeTestRule.onNodeWithText(titulo).performClick()
        composeTestRule.onNodeWithContentDescription("Inicio").performClick()
        composeTestRule.onNodeWithText("Consulta REAT").assertIsDisplayed()
    }
}
