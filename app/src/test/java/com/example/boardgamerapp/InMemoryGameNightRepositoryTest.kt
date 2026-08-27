package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryGameNightRepositoryTest {

    private val now = LocalDateTime.of(2026, 8, 26, 12, 0)
    private val host = Player(1, "Max", "Musterstraße 1", 1)

    @Test
    fun `returns the nearest planned game night with its host`() {
        val repository = InMemoryGameNightRepository(
            players = listOf(host),
            gameNights = listOf(
                gameNight(id = 2, startsAt = now.plusDays(2)),
                gameNight(id = 1, startsAt = now.plusDays(1)),
            ),
            now = { now },
        )

        val upcoming = repository.getUpcomingGameNight().getOrThrow()

        assertEquals(1L, upcoming?.gameNight?.id)
        assertEquals(host, upcoming?.host)
    }

    @Test
    fun `returns empty when no future planned game night exists`() {
        val repository = InMemoryGameNightRepository(
            players = listOf(host),
            gameNights = listOf(gameNight(startsAt = now.minusDays(1))),
            now = { now },
        )

        assertNull(repository.getUpcomingGameNight().getOrThrow())
    }

    @Test
    fun `returns failure when host is missing`() {
        val repository = InMemoryGameNightRepository(
            players = emptyList(),
            gameNights = listOf(gameNight(startsAt = now.plusDays(1))),
            now = { now },
        )

        assertTrue(repository.getUpcomingGameNight().isFailure)
    }

    private fun gameNight(
        id: Long = 1,
        startsAt: LocalDateTime,
    ) = GameNight(
        id = id,
        startsAt = startsAt,
        hostId = host.id,
        location = host.address,
        status = GameNightStatus.PLANNED,
    )
}
