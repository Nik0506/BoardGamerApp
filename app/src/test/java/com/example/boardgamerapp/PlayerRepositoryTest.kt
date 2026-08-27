package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRepositoryTest {

    private val max = Player(1, "Max", "Adresse 1", 1)
    private val lea = Player(2, "Lea", "Adresse 2", 2)
    private val firstGameNight = GameNight(
        id = 1,
        startsAt = LocalDateTime.of(2026, 8, 28, 19, 0),
        hostId = max.id,
        location = max.address,
        status = GameNightStatus.PLANNED,
    )

    @Test
    fun `adds player at end of host order and trims input`() {
        val repository = repository()

        val player = repository.addPlayer("  Tom  ", "  Adresse 3  ").getOrThrow()

        assertEquals("Tom", player.name)
        assertEquals("Adresse 3", player.address)
        assertEquals(3, player.hostOrder)
    }

    @Test
    fun `rejects blank player name`() {
        assertTrue(repository().addPlayer("   ", "Adresse").isFailure)
    }

    @Test
    fun `updates existing player without changing order`() {
        val repository = repository()

        val updated = repository.updatePlayer(lea.id, "Lea Neu", "Neue Adresse").getOrThrow()

        assertEquals("Lea Neu", updated.name)
        assertEquals(2, updated.hostOrder)
    }

    @Test
    fun `moves player and normalizes host order`() {
        val repository = repository()

        val ordered = repository.movePlayer(lea.id, MoveDirection.UP).getOrThrow()

        assertEquals(listOf(lea.id, max.id), ordered.map { it.id })
        assertEquals(listOf(1, 2), ordered.map { it.hostOrder })
    }

    @Test
    fun `creates next game night two weeks later with rotated host`() {
        val repository = repository()

        val upcoming = repository.createNextGameNight().getOrThrow()

        assertEquals(LocalDateTime.of(2026, 9, 11, 19, 0), upcoming.gameNight.startsAt)
        assertEquals(lea.id, upcoming.host.id)
        assertEquals(lea.address, upcoming.gameNight.location)
    }

    @Test
    fun `cannot create game night without players`() {
        val repository = InMemoryGameNightRepository(
            players = emptyList(),
            gameNights = emptyList(),
            now = { LocalDateTime.of(2026, 8, 26, 12, 0) },
        )

        assertTrue(repository.createNextGameNight().isFailure)
    }

    private fun repository() = InMemoryGameNightRepository(
        players = listOf(max, lea),
        gameNights = listOf(firstGameNight),
        now = { LocalDateTime.of(2026, 8, 26, 12, 0) },
    )
}
