package com.example.boardgamerapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.boardgamerapp.ui.review.RatingEditorUiState
import com.example.boardgamerapp.ui.review.ReviewPlayerUiModel
import com.example.boardgamerapp.ui.review.ReviewScreen
import com.example.boardgamerapp.ui.review.ReviewUiState
import org.junit.Rule
import org.junit.Test

class ReviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun finishAndOpenReviewDialogIsAccessible() {
        composeRule.setContent {
            var state by remember { mutableStateOf(content(isFinished = false)) }
            ReviewScreen(
                uiState = state,
                onRetry = {},
                onFinishGameNight = { state = state.copy(isFinished = true) },
                onBeginReview = { state = state.copy(editor = RatingEditorUiState()) },
                onHostRating = {},
                onFoodRating = {},
                onEveningRating = {},
                onCommentChange = {},
                onSaveReview = {},
                onDismissEditor = {},
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Spieleabend abschließen").performClick()
        composeRule.onNodeWithText("Bewertung abgeben").performClick()

        composeRule.onNodeWithText("Spieleabend bewerten").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Gastgeber: 5 von 5 Punkten").assertIsDisplayed()
        composeRule.onNodeWithText("Kommentar (optional)").assertIsDisplayed()
    }

    private fun content(isFinished: Boolean) = ReviewUiState.Content(
        gameNightId = 1,
        date = "28.08.2026, 19:00 Uhr",
        hostName = "Max",
        isFinished = isFinished,
        players = listOf(ReviewPlayerUiModel(1, "Max", false)),
        selectedPlayerId = 1,
        currentPlayerName = "Max",
        currentPlayerHasReviewed = false,
        reviewCount = 0,
        averages = null,
    )
}
