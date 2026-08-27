package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Vote
import com.example.boardgamerapp.ui.games.GamesViewModel
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamesViewModelTest {

    private val now = LocalDateTime.of(2026, 8, 27, 12, 0)
    private val max = Player(1, "Max", "Adresse 1", 1)
    private val lea = Player(2, "Lea", "Adresse 2", 2)
    private val gameNight = GameNight(
        id = 1,
        startsAt = LocalDateTime.of(2026, 8, 28, 19, 0),
        hostId = max.id,
        location = max.address,
        status = GameNightStatus.PLANNED,
    )
    private val catan = BoardGame(1, "Catan", "Handel", max.id, gameNight.id)
    private val heat = BoardGame(2, "Heat", "Rennen", lea.id, gameNight.id)

    @Test
    fun `shows participation tie and selected vote for active player`() {
        val viewModel = viewModel()

        assertEquals(2, viewModel.uiState.totalVotes)
        assertEquals(2, viewModel.uiState.playerCount)
        assertEquals("Gleichstand: Catan und Heat", viewModel.uiState.resultText)
        assertTrue(viewModel.uiState.suggestions.first { it.name == "Catan" }.isSelected)

        viewModel.selectPlayer(lea.id)

        assertTrue(viewModel.uiState.suggestions.first { it.name == "Heat" }.isSelected)
        assertFalse(viewModel.uiState.suggestions.first { it.name == "Catan" }.isSelected)
    }

    @Test
    fun `changing vote updates winner and sorted results immediately`() {
        val viewModel = viewModel()

        viewModel.castVote(heat.id)

        assertEquals("Aktuell vorne: Heat", viewModel.uiState.resultText)
        assertEquals("Heat", viewModel.uiState.suggestions.first().name)
        assertEquals(2, viewModel.uiState.suggestions.first().voteCount)
        assertEquals(2, viewModel.uiState.totalVotes)
    }

    private fun viewModel(): GamesViewModel {
        val repository = InMemoryGameNightRepository(
            players = listOf(max, lea),
            gameNights = listOf(gameNight),
            boardGames = listOf(catan, heat),
            votes = listOf(
                Vote(1, max.id, catan.id, gameNight.id),
                Vote(2, lea.id, heat.id, gameNight.id),
            ),
            now = { now },
        )
        return GamesViewModel(repository, repository, repository)
    }
}
