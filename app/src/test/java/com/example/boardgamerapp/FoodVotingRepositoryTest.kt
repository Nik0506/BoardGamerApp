package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodVotingRepositoryTest {
    private val max = Player(1, "Max", "Adresse 1", 1)
    private val lea = Player(2, "Lea", "Adresse 2", 2)
    private val night = GameNight(1, LocalDateTime.of(2026, 9, 4, 19, 0), 1, max.address, GameNightStatus.PLANNED)

    @Test
    fun `provides defaults and replaces vote per player and night`() {
        val repository = repository()
        val initial = repository.getFoodVotingSnapshot().getOrThrow()!!
        assertEquals(listOf("Asiatisch", "Burger", "Pizza"), initial.results.map { it.category.name })

        repository.castFoodVote(max.id, initial.results[0].category.id).getOrThrow()
        repository.castFoodVote(max.id, initial.results[1].category.id).getOrThrow()

        val snapshot = repository.getFoodVotingSnapshot().getOrThrow()!!
        assertEquals(1, snapshot.totalVotes)
        assertTrue(max.id in snapshot.results.first { it.category.id == initial.results[1].category.id }.voterIds)
    }

    @Test
    fun `adds and deletes category with its votes`() {
        val repository = repository()
        repository.getFoodVotingSnapshot().getOrThrow()
        val category = repository.addFoodCategory("Tapas").getOrThrow()
        repository.castFoodVote(lea.id, category.id).getOrThrow()
        repository.deleteFoodCategory(category.id).getOrThrow()

        val snapshot = repository.getFoodVotingSnapshot().getOrThrow()!!
        assertTrue(snapshot.results.none { it.category.id == category.id })
        assertTrue(lea in snapshot.missingPlayers)
    }

    @Test
    fun `reports missing players and tie`() {
        val repository = repository()
        val categories = repository.getFoodVotingSnapshot().getOrThrow()!!.results
        repository.castFoodVote(max.id, categories[0].category.id).getOrThrow()
        repository.castFoodVote(lea.id, categories[1].category.id).getOrThrow()

        val snapshot = repository.getFoodVotingSnapshot().getOrThrow()!!
        assertTrue(snapshot.missingPlayers.isEmpty())
        assertEquals(2, snapshot.results.count { it.voteCount == 1 })
    }

    @Test
    fun `rejects empty and duplicate category`() {
        val repository = repository()
        repository.getFoodVotingSnapshot().getOrThrow()
        assertTrue(repository.addFoodCategory(" ").isFailure)
        assertTrue(repository.addFoodCategory("pizza").isFailure)
    }

    private fun repository() = InMemoryGameNightRepository(
        players = listOf(max, lea), gameNights = listOf(night), now = { LocalDateTime.of(2026, 8, 29, 12, 0) },
    )
}
