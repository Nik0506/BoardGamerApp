package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.MockGameNightRepository
import com.example.boardgamerapp.domain.model.GameNightStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockGameNightRepositoryTest {

    @Test
    fun `provides players in host order`() = runBlocking {
        val players = MockGameNightRepository().getPlayers()

        assertEquals(listOf("Alex", "Sam", "Chris"), players.map { it.name })
        assertEquals(listOf(1, 2, 3), players.map { it.hostOrder })
    }

    @Test
    fun `provides a planned game night in the future`() = runBlocking {
        val now = 1_000_000L
        val gameNight = MockGameNightRepository { now }.getNextGameNight()

        assertEquals(GameNightStatus.PLANNED, gameNight.status)
        assertEquals(now + 7 * 24 * 60 * 60 * 1_000L, gameNight.startsAtEpochMillis)
        assertEquals("player-1", gameNight.hostId)
        assertTrue(gameNight.location.isNotBlank())
    }
}
