package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.GameNightRepository
import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.ui.dashboard.DashboardUiState
import com.example.boardgamerapp.ui.dashboard.DashboardViewModel
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardViewModelTest {

    @Test
    fun `maps repository data to content state`() {
        val host = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val gameNight = GameNight(
            id = 1,
            startsAt = LocalDateTime.of(2026, 8, 28, 19, 0),
            hostId = host.id,
            location = "Musterstraße 12, 33100 Paderborn",
            status = GameNightStatus.PLANNED,
        )
        val viewModel = DashboardViewModel(
            repository = repositoryReturning(Result.success(UpcomingGameNight(gameNight, host))),
        )

        val state = viewModel.uiState as DashboardUiState.Content

        assertEquals("Freitag, 28. August 2026", state.gameNight.date)
        assertEquals("19:00 Uhr", state.gameNight.time)
        assertEquals("Max Mustermann", state.gameNight.hostName)
    }

    @Test
    fun `maps missing game night to empty state`() {
        val viewModel = DashboardViewModel(repositoryReturning(Result.success(null)))

        assertEquals(DashboardUiState.Empty, viewModel.uiState)
    }

    @Test
    fun `maps repository failure to error state`() {
        val viewModel = DashboardViewModel(
            repositoryReturning(Result.failure(IllegalStateException("Testfehler"))),
        )

        assertTrue(viewModel.uiState is DashboardUiState.Error)
        assertEquals("Testfehler", (viewModel.uiState as DashboardUiState.Error).message)
    }

    @Test
    fun `saves selected player custom late notice and reloads dashboard`() {
        val max = Player(1, "Max", "Adresse 1", 1)
        val lea = Player(2, "Lea", "Adresse 2", 2)
        val gameNight = GameNight(
            id = 1,
            startsAt = LocalDateTime.of(2026, 8, 28, 19, 0),
            hostId = max.id,
            location = max.address,
            status = GameNightStatus.PLANNED,
        )
        val repository = InMemoryGameNightRepository(
            players = listOf(max, lea),
            gameNights = listOf(gameNight),
            now = { LocalDateTime.of(2026, 8, 27, 12, 0) },
        )
        val viewModel = DashboardViewModel(repository, repository, repository)

        viewModel.selectPlayer(lea.id)
        viewModel.beginLateNotice()
        viewModel.updateLateNoticeCustomMinutes("25")
        viewModel.saveLateNotice()

        val state = viewModel.uiState as DashboardUiState.Content
        assertEquals(lea.id, state.selectedPlayerId)
        assertEquals(25, state.lateNotices.single().minutes)
        assertEquals("Lea", state.lateNotices.single().playerName)
        assertEquals("Verspätungsmeldung wurde lokal gespeichert.", state.message)
    }

    @Test
    fun `shows clear error for non positive custom minutes`() {
        val repository = InMemoryGameNightRepository(
            now = { LocalDateTime.of(2026, 8, 27, 12, 0) },
        )
        val viewModel = DashboardViewModel(repository, repository, repository)

        viewModel.beginLateNotice()
        viewModel.updateLateNoticeCustomMinutes("-5")
        viewModel.saveLateNotice()

        val state = viewModel.uiState as DashboardUiState.Content
        assertEquals("Gib eine positive Minutenzahl ein.", state.editor?.errorMessage)
        assertTrue(state.lateNotices.isEmpty())
    }

    private fun repositoryReturning(
        result: Result<UpcomingGameNight?>,
    ): GameNightRepository = object : GameNightRepository {
        override fun getUpcomingGameNight(): Result<UpcomingGameNight?> = result
    }
}
