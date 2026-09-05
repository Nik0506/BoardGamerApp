package com.example.boardgamerapp

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
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

    @Test
    fun emailAndRegistrationFields_acceptUmlautsAndSpecialCharacters() {
        // Switch to registration mode
        composeRule.onNode(hasText("Noch kein Konto? Registrieren"))
            .performClick()

        // Name with German umlauts and hyphen
        composeRule.onNode(hasText("Name") and hasSetTextAction())
            .performTextInput("Jörg Müller-Lüdenscheidt")

        // Address with German umlauts, ß, and special characters
        composeRule.onNode(hasText("Adresse") and hasSetTextAction())
            .performTextInput("Schloßstraße 42, 12345 Überlingen (Bodensee)")

        // Email with @ and special characters (+, -, .)
        composeRule.onNode(hasText("E-Mail") and hasSetTextAction())
            .performTextInput("test.user+board@gamer-app.de")

        // Assert all fields accept and contain the special characters
        composeRule.onNode(hasSetTextAction() and hasText("Jörg Müller-Lüdenscheidt"))
            .assertTextContains("Jörg Müller-Lüdenscheidt")

        composeRule.onNode(hasSetTextAction() and hasText("Schloßstraße 42, 12345 Überlingen (Bodensee)"))
            .assertTextContains("Schloßstraße 42, 12345 Überlingen (Bodensee)")

        composeRule.onNode(hasSetTextAction() and hasText("test.user+board@gamer-app.de"))
            .assertTextContains("test.user+board@gamer-app.de")
    }
}
