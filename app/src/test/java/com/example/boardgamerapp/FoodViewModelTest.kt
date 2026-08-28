package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.ui.food.FoodViewModel
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodViewModelTest {
    private val max = Player(1, "Max", "Adresse", 1)
    private val lea = Player(2, "Lea", "Adresse 2", 2)
    private val repository = InMemoryGameNightRepository(
        players = listOf(max, lea),
        gameNights = listOf(GameNight(1, LocalDateTime.of(2026, 9, 4, 19, 0), 1, max.address, GameNightStatus.PLANNED)),
        now = { LocalDateTime.of(2026, 8, 29, 12, 0) },
    )

    @Test
    fun `shows tie and missing voters after votes`() {
        val viewModel = FoodViewModel(repository)
        viewModel.castVote(viewModel.uiState.categories[0].id)
        viewModel.selectPlayer(lea.id)
        viewModel.castVote(viewModel.uiState.categories[1].id)

        assertTrue(viewModel.uiState.resultText.startsWith("Gleichstand:"))
        assertTrue(viewModel.uiState.missingPlayerNames.isEmpty())
    }

    @Test
    fun `local reminder names missing players`() {
        val viewModel = FoodViewModel(repository)
        viewModel.castVote(viewModel.uiState.categories[0].id)
        viewModel.remindMissingPlayers()

        assertTrue(viewModel.uiState.message.orEmpty().contains("Lea"))
        assertTrue(viewModel.uiState.message.orEmpty().contains("keine Nachricht"))
    }

    @Test
    fun `adds category from editor`() {
        val viewModel = FoodViewModel(repository)
        viewModel.beginAddCategory()
        viewModel.updateCategoryName("Tapas")
        viewModel.saveCategory()

        assertEquals(1, viewModel.uiState.categories.count { it.name == "Tapas" })
    }
}
