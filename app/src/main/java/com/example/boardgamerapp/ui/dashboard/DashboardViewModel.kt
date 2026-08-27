package com.example.boardgamerapp.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.boardgamerapp.data.repository.GameNightRepository
import com.example.boardgamerapp.data.repository.BoardGamerRepository
import com.example.boardgamerapp.data.repository.LateNoticeRepository
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.domain.model.LateNotice
import java.time.format.DateTimeFormatter
import java.util.Locale

class DashboardViewModel(
    private val repository: GameNightRepository,
    private val playerRepository: PlayerRepository? = null,
    private val lateNoticeRepository: LateNoticeRepository? = null,
) : ViewModel() {

    var uiState: DashboardUiState by mutableStateOf(DashboardUiState.Loading)
        private set

    init {
        loadGameNight()
    }

    fun loadGameNight() {
        val previousSelectedPlayerId = (uiState as? DashboardUiState.Content)?.selectedPlayerId
        uiState = DashboardUiState.Loading
        val upcomingResult = repository.getUpcomingGameNight()
        val upcoming = upcomingResult.getOrNull()
        if (upcomingResult.isFailure) {
            uiState = DashboardUiState.Error(
                message = upcomingResult.exceptionOrNull()?.message
                    ?: "Der nächste Spieleabend konnte nicht geladen werden.",
            )
            return
        }
        if (upcoming == null) {
            uiState = DashboardUiState.Empty
            return
        }

        val playersResult = playerRepository?.getPlayers() ?: Result.success(emptyList())
        val noticesResult = lateNoticeRepository?.getLateNotices() ?: Result.success(emptyList())
        val error = playersResult.exceptionOrNull() ?: noticesResult.exceptionOrNull()
        if (error != null) {
            uiState = DashboardUiState.Error(
                message = error.message ?: "Die Verspätungsmeldungen konnten nicht geladen werden.",
            )
            return
        }

        val players = playersResult.getOrThrow()
            .map { DashboardPlayerUiModel(id = it.id, name = it.name) }
        uiState = DashboardUiState.Content(
            gameNight = upcoming.toUiModel(),
            players = players,
            selectedPlayerId = selectedPlayer(players, previousSelectedPlayerId),
            lateNotices = noticesResult.getOrThrow().map { it.toUiModel(players) },
        )
    }

    fun selectPlayer(playerId: Long) {
        val content = uiState as? DashboardUiState.Content ?: return
        if (content.players.any { it.id == playerId }) {
            uiState = content.copy(selectedPlayerId = playerId, errorMessage = null)
        }
    }

    fun beginLateNotice() {
        val content = uiState as? DashboardUiState.Content ?: return
        if (content.selectedPlayerId == null) {
            uiState = content.copy(errorMessage = "Wähle zuerst den meldenden Spieler aus.")
            return
        }
        uiState = content.copy(editor = LateNoticeEditorUiState(), message = null, errorMessage = null)
    }

    fun selectLateNoticePreset(minutes: Int) {
        val content = uiState as? DashboardUiState.Content ?: return
        if (minutes in listOf(10, 20, 30)) {
            uiState = content.copy(
                editor = (content.editor ?: LateNoticeEditorUiState()).copy(
                    selectedPreset = minutes,
                    errorMessage = null,
                ),
            )
        }
    }

    fun updateLateNoticeCustomMinutes(minutes: String) {
        val content = uiState as? DashboardUiState.Content ?: return
        uiState = content.copy(
            editor = (content.editor ?: LateNoticeEditorUiState()).copy(
                selectedPreset = null,
                customMinutes = minutes,
                errorMessage = null,
            ),
        )
    }

    fun saveLateNotice() {
        val content = uiState as? DashboardUiState.Content ?: return
        val editor = content.editor ?: return
        val minutes = editor.selectedPreset ?: editor.customMinutes.toIntOrNull()
        if (minutes == null || minutes <= 0) {
            uiState = content.copy(
                editor = editor.copy(errorMessage = "Gib eine positive Minutenzahl ein."),
            )
            return
        }
        val playerId = content.selectedPlayerId
        if (playerId == null || lateNoticeRepository == null) {
            uiState = content.copy(errorMessage = "Wähle zuerst den meldenden Spieler aus.")
            return
        }
        lateNoticeRepository.addLateNotice(playerId, minutes).fold(
            onSuccess = {
                loadGameNight()
                val loaded = uiState as? DashboardUiState.Content
                if (loaded != null) {
                    uiState = loaded.copy(message = "Verspätungsmeldung wurde lokal gespeichert.")
                }
            },
            onFailure = { error ->
                uiState = content.copy(
                    editor = editor.copy(
                        errorMessage = error.message
                            ?: "Die Verspätungsmeldung konnte nicht gespeichert werden.",
                    ),
                )
            },
        )
    }

    fun dismissLateNoticeEditor() {
        val content = uiState as? DashboardUiState.Content ?: return
        uiState = content.copy(editor = null)
    }

    fun clearMessage() {
        val content = uiState as? DashboardUiState.Content ?: return
        uiState = content.copy(message = null, errorMessage = null)
    }

    private fun UpcomingGameNight.toUiModel(): GameNightUiModel = GameNightUiModel(
        date = gameNight.startsAt.format(dateFormatter),
        time = gameNight.startsAt.format(timeFormatter),
        hostName = host.name,
        location = gameNight.location,
    )

    private fun selectedPlayer(
        players: List<DashboardPlayerUiModel>,
        preferredPlayerId: Long?,
    ): Long? =
        preferredPlayerId
            ?.takeIf { id -> players.any { it.id == id } }
            ?: players.firstOrNull()?.id

    private fun LateNotice.toUiModel(players: List<DashboardPlayerUiModel>) = LateNoticeUiModel(
        id = id,
        playerName = players.firstOrNull { it.id == playerId }?.name ?: "Unbekannter Spieler",
        minutes = minutes,
        createdAt = createdAt.format(noticeDateFormatter),
    )

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm 'Uhr'", Locale.GERMAN)

        private val noticeDateFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMAN)

        fun factory(repository: GameNightRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
                    return DashboardViewModel(repository) as T
                }
            }

        fun factory(repository: BoardGamerRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
                    return DashboardViewModel(repository, repository, repository) as T
                }
            }
    }
}
