package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRepositoryTest {
    private val player = Player(1, "Max", "Adresse", 1)
    private val night = GameNight(
        1, LocalDateTime.of(2026, 8, 28, 19, 0), player.id, player.address, GameNightStatus.PLANNED,
    )

    @Test
    fun `only finished game night can be reviewed`() {
        val repository = repository()
        assertTrue(repository.submitReview(1, 1, 5, 4, 5, "Top").isFailure)

        repository.finishGameNight(1).getOrThrow()
        val review = repository.submitReview(1, 1, 5, 4, 3, " Top ").getOrThrow()

        assertEquals("Top", review.comment)
        assertEquals(GameNightStatus.FINISHED, repository.getReviewSnapshot().getOrThrow()?.gameNight?.status)
    }

    @Test
    fun `rejects duplicate and invalid ratings`() {
        val repository = repository()
        repository.finishGameNight(1).getOrThrow()
        assertTrue(repository.submitReview(1, 1, 0, 4, 5, "").isFailure)
        repository.submitReview(1, 1, 5, 4, 3, "").getOrThrow()
        assertTrue(repository.submitReview(1, 1, 4, 4, 4, "Noch einmal").isFailure)
    }

    @Test
    fun `calculates averages`() {
        val lea = Player(2, "Lea", "Adresse 2", 2)
        val repository = InMemoryGameNightRepository(
            players = listOf(player, lea), gameNights = listOf(night), now = { LocalDateTime.of(2026, 8, 28, 12, 0) },
        )
        repository.finishGameNight(1).getOrThrow()
        repository.submitReview(1, 1, 5, 3, 4, "").getOrThrow()
        repository.submitReview(2, 1, 3, 5, 2, "").getOrThrow()

        val averages = repository.getReviewSnapshot().getOrThrow()?.averages
        assertEquals(4.0, averages?.host ?: 0.0, 0.0)
        assertEquals(4.0, averages?.food ?: 0.0, 0.0)
        assertEquals(3.0, averages?.evening ?: 0.0, 0.0)
    }

    private fun repository() = InMemoryGameNightRepository(
        players = listOf(player), gameNights = listOf(night), now = { LocalDateTime.of(2026, 8, 28, 12, 0) },
    )
}
