package com.example.boardgamerapp.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.boardgamerapp.data.repository.GameNightRepository
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import java.time.format.DateTimeFormatter
import java.util.Locale

class DashboardViewModel(
    private val repository: GameNightRepository,
) : ViewModel() {

    var uiState: DashboardUiState by mutableStateOf(DashboardUiState.Loading)
        private set

    init {
        loadGameNight()
    }

    fun loadGameNight() {
        uiState = DashboardUiState.Loading
        uiState = repository.getUpcomingGameNight().fold(
            onSuccess = { upcomingGameNight ->
                upcomingGameNight?.let {
                    DashboardUiState.Content(it.toUiModel())
                } ?: DashboardUiState.Empty
            },
            onFailure = {
                DashboardUiState.Error(
                    message = it.message ?: "Der nächste Spieleabend konnte nicht geladen werden.",
                )
            },
        )
    }

    private fun UpcomingGameNight.toUiModel(): GameNightUiModel = GameNightUiModel(
        date = gameNight.startsAt.format(dateFormatter),
        time = gameNight.startsAt.format(timeFormatter),
        hostName = host.name,
        location = gameNight.location,
    )

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm 'Uhr'", Locale.GERMAN)

        fun factory(repository: GameNightRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
                    return DashboardViewModel(repository) as T
                }
            }
    }
}
