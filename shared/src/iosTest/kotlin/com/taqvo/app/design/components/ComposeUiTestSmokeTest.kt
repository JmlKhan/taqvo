package com.taqvo.app.design.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.app_name
import kotlin.test.Test

/** Proves the harness: a scene composes under test, and — second test — Compose resources load. */
@OptIn(ExperimentalTestApi::class)
class ComposeUiTestSmokeTest {

    @Test
    fun aComposedTextIsFoundByItsText() = runComposeUiTest {
        setContent { Text("Taqvo") }
        onNodeWithText("Taqvo").assertIsDisplayed()
    }

    @Test
    fun aStringResourceLoadsUnderTest() = runComposeUiTest {
        // Read from the resource, not a literal: the simulator's language decides what the app
        // is called, and the test only has to prove that resources load at all.
        val name = getString(Res.string.app_name)
        setContent { Text(stringResource(Res.string.app_name)) }
        onNodeWithText(name).assertIsDisplayed()
    }
}
