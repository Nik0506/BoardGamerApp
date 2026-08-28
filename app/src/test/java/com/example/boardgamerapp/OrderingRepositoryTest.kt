package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderingRepositoryTest {
    private val max = Player(1, "Max", "Adresse", 1)
    private val lea = Player(2, "Lea", "Adresse 2", 2)
    private fun repository() = InMemoryGameNightRepository(
        players = listOf(max, lea),
        gameNights = listOf(GameNight(1, LocalDateTime.of(2026, 9, 4, 19, 0), max.id, max.address, GameNightStatus.PLANNED)),
        now = { LocalDateTime.of(2026, 8, 29, 12, 0) },
    )

    @Test fun `only host can save restaurant`() {
        val repository = repository()
        assertTrue(repository.saveRestaurant(lea.id, "Pizza Haus", "https://menu.example").isFailure)
        repository.saveRestaurant(max.id, "Pizza Haus", "https://menu.example").getOrThrow()
        assertEquals("Pizza Haus", repository.getOrderingSnapshot().getOrThrow()?.restaurant?.name)
    }

    @Test fun `order is replaced per player and cents are summed exactly`() {
        val repository = repository()
        repository.saveFoodOrder(max.id, "Pizza", "ohne Oliven", 1099).getOrThrow()
        repository.saveFoodOrder(max.id, "Pasta", "", 1250).getOrThrow()
        repository.saveFoodOrder(lea.id, "Salat", "", 875).getOrThrow()
        val snapshot = repository.getOrderingSnapshot().getOrThrow()!!
        assertEquals(2, snapshot.orders.size)
        assertEquals(2125, snapshot.totalCents)
        assertEquals("Pasta", snapshot.orders.first { it.player.id == max.id }.order.dish)
    }

    @Test fun `only own order can be deleted`() {
        val repository = repository()
        val order = repository.saveFoodOrder(max.id, "Pizza", "", 1000).getOrThrow()
        assertTrue(repository.deleteFoodOrder(order.id, lea.id).isFailure)
        repository.deleteFoodOrder(order.id, max.id).getOrThrow()
        assertTrue(repository.getOrderingSnapshot().getOrThrow()!!.orders.isEmpty())
    }
}
