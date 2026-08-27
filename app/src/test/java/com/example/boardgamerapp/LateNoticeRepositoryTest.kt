package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.LateNotice
import com.example.boardgamerapp.domain.model.Player
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LateNoticeRepositoryTest {

    private val now = LocalDateTime.of(2026, 8, 27, 12, 0)
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
    fun `saves notice for selected player and upcoming game night`() {
        val repository = repository()

        val notice = repository.addLateNotice(lea.id, 20).getOrThrow()

        assertEquals(lea.id, notice.playerId)
        assertEquals(gameNight.id, notice.gameNightId)
        assertEquals(20, notice.minutes)
        assertEquals(listOf(notice), repository.getLateNotices().getOrThrow())
    }

    @Test
    fun `rejects zero and negative minutes`() {
        val repository = repository()

        assertEquals(
            "Die Verspätung muss größer als 0 Minuten sein.",
            repository.addLateNotice(max.id, 0).exceptionOrNull()?.message,
        )
        assertEquals(
            "Die Verspätung muss größer als 0 Minuten sein.",
            repository.addLateNotice(max.id, -10).exceptionOrNull()?.message,
        )
        assertTrue(repository.getLateNotices().getOrThrow().isEmpty())
    }

    @Test
    fun `rejects unknown player`() {
        assertTrue(repository().addLateNotice(999, 10).isFailure)
    }

    @Test
    fun `returns notices for upcoming night only`() {
        val oldNotice = LateNotice(1, max.id, 999, 30, now)
        val repository = InMemoryGameNightRepository(
            players = listOf(max, lea),
            gameNights = listOf(gameNight),
            lateNotices = listOf(oldNotice),
            now = { now },
        )

        assertTrue(repository.getLateNotices().getOrThrow().isEmpty())
    }

    private fun repository() = InMemoryGameNightRepository(
        players = listOf(max, lea),
        gameNights = listOf(gameNight),
        now = { now },
    )
}
