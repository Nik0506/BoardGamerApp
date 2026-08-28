package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.ui.review.ReviewUiState
import com.example.boardgamerapp.ui.review.ReviewViewModel
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewViewModelTest {
    @Test
    fun `finishes night and saves complete rating`() {
        val player = Player(1, "Max", "Adresse", 1)
        val repository = InMemoryGameNightRepository(
            players = listOf(player),
            gameNights = listOf(GameNight(1, LocalDateTime.of(2026, 8, 28, 19, 0), 1, "Adresse", GameNightStatus.PLANNED)),
            now = { LocalDateTime.of(2026, 8, 28, 12, 0) },
        )
        val viewModel = ReviewViewModel(repository, repository)

        viewModel.finishGameNight()
        viewModel.beginReview()
        viewModel.setHostRating(5)
        viewModel.setFoodRating(4)
        viewModel.setEveningRating(3)
        viewModel.updateComment("Schöner Abend")
        viewModel.saveReview()

        val state = viewModel.uiState as ReviewUiState.Content
        assertEquals(1, state.reviewCount)
        assertEquals("5,0", state.averages?.host)
        assertNull(state.selectedPlayerId)
        assertEquals("Bewertung gespeichert.", state.message)
    }

    @Test
    fun `requires all three ratings`() {
        val player = Player(1, "Max", "Adresse", 1)
        val finished = GameNight(1, LocalDateTime.of(2026, 8, 28, 19, 0), 1, "Adresse", GameNightStatus.FINISHED)
        val repository = InMemoryGameNightRepository(players = listOf(player), gameNights = listOf(finished))
        val viewModel = ReviewViewModel(repository, repository)

        viewModel.beginReview()
        viewModel.setHostRating(5)
        viewModel.saveReview()

        val state = viewModel.uiState as ReviewUiState.Content
        assertEquals("Vergib in allen drei Kategorien 1 bis 5 Punkte.", state.editor?.errorMessage)
        assertEquals(0, state.reviewCount)
    }
}
