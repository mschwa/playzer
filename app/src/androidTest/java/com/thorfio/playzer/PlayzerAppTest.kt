package com.thorfio.playzer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class PlayzerAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun helloText_isVisible() {
        composeRule.onNodeWithText("Hello Jetpack Compose 👋").assertIsDisplayed()
    }

    @Test
    fun darkToggle_changesLabel() {
        // Initial state: dark=false so button text should be "Switch Dark"
        val darkButtonInitial = composeRule.onNodeWithText("Switch Dark")
        darkButtonInitial.assertIsDisplayed()
        darkButtonInitial.performClick()
        // After click: dark=true so label flips to "Switch Light"
        composeRule.onNodeWithText("Switch Light").assertIsDisplayed()
    }
}

