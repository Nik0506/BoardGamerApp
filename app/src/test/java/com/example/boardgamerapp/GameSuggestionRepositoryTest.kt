package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSuggestionRepositoryTest {

    private val now = LocalDateTime.of(2026, 8, 26, 12, 0)
    private val max = Player(1, "Max", "Adresse 1", 1)
    private val lea = Player(2, "Lea", "Adresse 2", 2)
    private val gameNight = GameNight(
        id = 10,
        startsAt = LocalDateTime.of(2026, 8, 28, 19, 0),
        hostId = max.id,
        location = max.address,
        status = GameNightStatus.PLANNED,
    )

    @Test
    fun `adds trimmed suggestion for selected player and upcoming game night`() {
        val repository = repository()

        val suggestion = repository.addGameSuggestion(
            name = "  Catan  ",
            description = "  Handel und Aufbau  ",
            suggestedByPlayerId = lea.id,
        ).getOrThrow()

        assertEquals("Catan", suggestion.boardGame.name)
        assertEquals("Handel und Aufbau", suggestion.boardGame.description)
        assertEquals(lea, suggestion.suggestedBy)
        assertEquals(gameNight.id, suggestion.boardGame.gameNightId)
    }

    @Test
    fun `rejects blank game name`() {
        val result = repository().addGameSuggestion("   ", "Beschreibung", max.id)

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects suggestion when there is no upcoming game night`() {
        val repository = InMemoryGameNightRepository(
            players = listOf(max),
            gameNights = emptyList(),
            boardGames = emptyList(),
            now = { now },
        )

        assertTrue(repository.addGameSuggestion("Heat", "", max.id).isFailure)
    }

    @Test
    fun `returns suggestions with author for upcoming game night only`() {
        val repository = repository(
            boardGames = listOf(
                BoardGame(1, "Heat", "Rennen", lea.id, gameNight.id),
                BoardGame(2, "Alt", "", max.id, 999),
            ),
        )

        val result = repository.getGameSuggestions().getOrThrow()

        assertEquals(gameNight, result?.gameNight)
        assertEquals(listOf("Heat"), result?.suggestions?.map { it.boardGame.name })
        assertEquals(lea, result?.suggestions?.single()?.suggestedBy)
    }

    @Test
    fun `owner can delete own suggestion`() {
        val game = BoardGame(1, "Heat", "Rennen", lea.id, gameNight.id)
        val repository = repository(boardGames = listOf(game))

        repository.deleteGameSuggestion(game.id, lea.id).getOrThrow()

        assertTrue(repository.getGameSuggestions().getOrThrow()?.suggestions?.isEmpty() == true)
    }

    @Test
    fun `another player cannot delete suggestion`() {
        val game = BoardGame(1, "Heat", "Rennen", lea.id, gameNight.id)
        val repository = repository(boardGames = listOf(game))

        assertTrue(repository.deleteGameSuggestion(game.id, max.id).isFailure)
        assertEquals(1, repository.getGameSuggestions().getOrThrow()?.suggestions?.size)
    }

    private fun repository(
        boardGames: List<BoardGame> = emptyList(),
    ) = InMemoryGameNightRepository(
        players = listOf(max, lea),
        gameNights = listOf(gameNight),
        boardGames = boardGames,
        now = { now },
    )
}
