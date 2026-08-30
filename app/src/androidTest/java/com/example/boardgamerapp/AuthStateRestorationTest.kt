package com.example.boardgamerapp

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTextInput
import com.google.firebase.auth.FirebaseAuth
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthStateRestorationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun showLogin() {
        FirebaseAuth.getInstance().signOut()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @Test
    fun emailInputSurvivesSavedInstanceStateRestoration() {
        composeRule.onNode(hasText("E-Mail") and hasSetTextAction())
            .performTextInput("rotation@test.de")

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction() and hasText("rotation@test.de"))
            .assertTextContains("rotation@test.de")
    }
}
