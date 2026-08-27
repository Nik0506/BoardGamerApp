package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Vote
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VotingRepositoryTest {

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
    fun `counts one vote per player and game night`() {
        val repository = repository(
            votes = listOf(
                Vote(1, max.id, catan.id, gameNight.id),
                Vote(2, lea.id, heat.id, gameNight.id),
            ),
        )

        val snapshot = repository.getVotingSnapshot().getOrThrow()

        assertEquals(2, snapshot?.totalVotes)
        assertEquals(1, snapshot?.results?.first { it.suggestion.boardGame == catan }?.voteCount)
        assertEquals(1, snapshot?.results?.first { it.suggestion.boardGame == heat }?.voteCount)
    }

    @Test
    fun `changing vote replaces previous vote instead of adding another`() {
        val repository = repository(
            votes = listOf(
                Vote(1, max.id, catan.id, gameNight.id),
                Vote(2, lea.id, heat.id, gameNight.id),
            ),
        )

        repository.castVote(max.id, heat.id).getOrThrow()
        val snapshot = repository.getVotingSnapshot().getOrThrow()

        assertEquals(2, snapshot?.totalVotes)
        assertEquals(2, snapshot?.results?.first()?.voteCount)
        assertEquals("Heat", snapshot?.results?.first()?.suggestion?.boardGame?.name)
        assertEquals(0, snapshot?.results?.last()?.voteCount)
    }

    @Test
    fun `repeated vote for same game remains one vote`() {
        val repository = repository()

        repository.castVote(max.id, catan.id).getOrThrow()
        repository.castVote(max.id, catan.id).getOrThrow()

        assertEquals(1, repository.getVotingSnapshot().getOrThrow()?.totalVotes)
    }

    @Test
    fun `rejects unknown player`() {
        assertTrue(repository().castVote(999, catan.id).isFailure)
    }

    @Test
    fun `rejects game from another game night`() {
        val otherGame = BoardGame(3, "Azul", "", max.id, 999)
        val repository = repository(boardGames = listOf(catan, heat, otherGame))

        assertTrue(repository.castVote(max.id, otherGame.id).isFailure)
    }

    @Test
    fun `deleting suggestion also removes its votes`() {
        val repository = repository(votes = listOf(Vote(1, max.id, catan.id, gameNight.id)))

        repository.deleteGameSuggestion(catan.id, max.id).getOrThrow()

        assertEquals(0, repository.getVotingSnapshot().getOrThrow()?.totalVotes)
    }

    private fun repository(
        boardGames: List<BoardGame> = listOf(catan, heat),
        votes: List<Vote> = emptyList(),
    ) = InMemoryGameNightRepository(
        players = listOf(max, lea),
        gameNights = listOf(gameNight),
        boardGames = boardGames,
        votes = votes,
        now = { now },
    )
}
