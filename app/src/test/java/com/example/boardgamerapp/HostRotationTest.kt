package com.example.boardgamerapp

import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.nextHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HostRotationTest {
    private val players = listOf(
        Player("a", "Alex", "A", 1),
        Player("b", "Sam", "B", 2),
        Player("c", "Chris", "C", 3),
    )

    @Test
    fun `starts with first player when no last host exists`() {
        assertEquals("a", nextHost(players, null)?.id)
    }

    @Test
    fun `wraps to first player after last host`() {
        assertEquals("a", nextHost(players, "c")?.id)
    }

    @Test
    fun `returns null for empty player list`() {
        assertNull(nextHost(emptyList(), null))
    }
}
