package com.example.boardgamerapp

import com.example.boardgamerapp.domain.HostRotation
import com.example.boardgamerapp.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HostRotationTest {

    private val players = listOf(
        Player(2, "Lea", "Adresse 2", 2),
        Player(1, "Max", "Adresse 1", 1),
        Player(3, "Tom", "Adresse 3", 3),
    )

    @Test
    fun `returns first player when there was no previous host`() {
        assertEquals(1L, HostRotation.nextHost(players, null)?.id)
    }

    @Test
    fun `returns player after previous host in configured order`() {
        assertEquals(2L, HostRotation.nextHost(players, 1)?.id)
    }

    @Test
    fun `wraps from last player to first player`() {
        assertEquals(1L, HostRotation.nextHost(players, 3)?.id)
    }

    @Test
    fun `supports a group with one player`() {
        assertEquals(1L, HostRotation.nextHost(listOf(players[1]), 1)?.id)
    }

    @Test
    fun `returns null for empty group`() {
        assertNull(HostRotation.nextHost(emptyList(), null))
    }
}
