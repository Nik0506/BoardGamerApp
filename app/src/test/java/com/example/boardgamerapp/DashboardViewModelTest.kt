package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.GameNightRepository
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.ui.dashboard.DashboardUiState
import com.example.boardgamerapp.ui.dashboard.DashboardViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `maps repository data to content state`() = runTest(dispatcher) {
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
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        val state = viewModel.uiState as DashboardUiState.Content

        assertEquals("Freitag, 28. August 2026", state.gameNight.date)
        assertEquals("19:00 Uhr", state.gameNight.time)
        assertEquals("Max Mustermann", state.gameNight.hostName)
    }

    @Test
    fun `maps missing game night to empty state`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(repositoryReturning(Result.success(null)), ioDispatcher = dispatcher)
        advanceUntilIdle()

        assertEquals(DashboardUiState.Empty, viewModel.uiState)
    }

    @Test
    fun `maps repository failure to error state`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(
            repositoryReturning(Result.failure(IllegalStateException("Testfehler"))),
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState is DashboardUiState.Error)
        assertEquals("Testfehler", (viewModel.uiState as DashboardUiState.Error).message)
    }

    private fun repositoryReturning(
        result: Result<UpcomingGameNight?>,
    ): GameNightRepository = object : GameNightRepository {
        override fun getUpcomingGameNight(): Result<UpcomingGameNight?> = result
    }
}
