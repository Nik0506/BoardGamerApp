package com.example.boardgamerapp.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.boardgamerapp.data.repository.GameNightRepository
import com.example.boardgamerapp.data.repository.BoardGamerRepository
import com.example.boardgamerapp.data.repository.LateNoticeRepository
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.domain.model.LateNotice
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(
    private val repository: GameNightRepository,
    private val playerRepository: PlayerRepository? = null,
    private val lateNoticeRepository: LateNoticeRepository? = null,
    private val currentPlayerId: Long? = null,
    private val onSendNotification: ((title: String, message: String) -> Unit)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    var uiState: DashboardUiState by mutableStateOf(DashboardUiState.Loading)
        private set

    init {
        loadGameNight()
    }

    fun loadGameNight() {
        uiState = DashboardUiState.Loading
        viewModelScope.launch {
            fetchGameNightContent()
        }
    }

    private suspend fun fetchGameNightContent(successMessage: String? = null) {
        val upcomingResult = withContext(ioDispatcher) { repository.getUpcomingGameNight() }
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

        val playersResult = withContext(ioDispatcher) { resolvePlayersForDashboard() }
        val noticesResult = withContext(ioDispatcher) { lateNoticeRepository?.getLateNotices() ?: Result.success(emptyList()) }
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
            selectedPlayerId = currentPlayerId?.takeIf { id -> players.any { it.id == id } },
            lateNotices = noticesResult.getOrThrow().map { it.toUiModel(players) },
            message = successMessage,
        )
    }

    fun beginEditGameNight() {
        val content = uiState as? DashboardUiState.Content ?: return
        val date = content.gameNight.startsAt?.toLocalDate() ?: java.time.LocalDate.now().plusWeeks(2)
        val time = content.gameNight.startsAt?.toLocalTime() ?: java.time.LocalTime.of(19, 0)
        val hostId = content.gameNight.hostId.takeIf { it != 0L }
            ?: content.players.firstOrNull()?.id
            ?: 0L
        uiState = content.copy(
            gameNightEditor = GameNightEditorUiState(
                gameNightId = content.gameNight.id,
                selectedDate = date,
                selectedTime = time,
                selectedHostId = hostId,
            ),
            message = null,
            errorMessage = null,
        )
    }

    fun updateGameNightEditorDate(date: java.time.LocalDate) {
        val content = uiState as? DashboardUiState.Content ?: return
        val currentEditor = content.gameNightEditor ?: return
        uiState = content.copy(
            gameNightEditor = currentEditor.copy(selectedDate = date, errorMessage = null),
        )
    }

    fun updateGameNightEditorTime(time: java.time.LocalTime) {
        val content = uiState as? DashboardUiState.Content ?: return
        val currentEditor = content.gameNightEditor ?: return
        uiState = content.copy(
            gameNightEditor = currentEditor.copy(selectedTime = time, errorMessage = null),
        )
    }

    fun updateGameNightEditorHost(hostId: Long) {
        val content = uiState as? DashboardUiState.Content ?: return
        val currentEditor = content.gameNightEditor ?: return
        uiState = content.copy(
            gameNightEditor = currentEditor.copy(selectedHostId = hostId, errorMessage = null),
        )
    }

    fun dismissGameNightEditor() {
        val content = uiState as? DashboardUiState.Content ?: return
        uiState = content.copy(gameNightEditor = null)
    }

    fun saveEditedGameNight() {
        val content = uiState as? DashboardUiState.Content ?: return
        val editor = content.gameNightEditor ?: return
        if (editor.selectedHostId == 0L) {
            uiState = content.copy(
                gameNightEditor = editor.copy(errorMessage = "Bitte wähle einen Gastgeber aus."),
            )
            return
        }

        val startsAt = java.time.LocalDateTime.of(editor.selectedDate, editor.selectedTime)
        uiState = content.copy(gameNightEditor = editor.copy(isSaving = true, errorMessage = null))

        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                repository.updateGameNight(
                    gameNightId = editor.gameNightId,
                    startsAt = startsAt,
                    hostPlayerId = editor.selectedHostId,
                )
            }
            result.fold(
                onSuccess = { updated ->
                    onSendNotification?.invoke(
                        "Spieleabend aktualisiert",
                        "Neuer Termin: ${startsAt.format(dateFormatter)}, ${startsAt.format(timeFormatter)} bei ${updated.host.name}",
                    )
                    fetchGameNightContent(
                        successMessage = "Spieleabend wurde erfolgreich aktualisiert. Teilnehmer wurden per Push-Nachricht informiert.",
                    )
                },
                onFailure = { error ->
                    uiState = content.copy(
                        gameNightEditor = editor.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Der Spieleabend konnte nicht aktualisiert werden.",
                        ),
                    )
                },
            )
        }
    }

    fun beginLateNotice() {
        val content = uiState as? DashboardUiState.Content ?: return
        if (content.selectedPlayerId == null) {
            uiState = content.copy(errorMessage = "Dein Konto ist kein Mitglied der aktiven Gruppe.")
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
            uiState = content.copy(errorMessage = "Dein Konto ist kein Mitglied der aktiven Gruppe.")
            return
        }
        viewModelScope.launch {
        withContext(ioDispatcher) { lateNoticeRepository.addLateNotice(playerId, minutes) }.fold(
            onSuccess = {
                fetchGameNightContent(successMessage = "Verspätungsmeldung wurde gespeichert.")
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
    }

    fun dismissLateNoticeEditor() {
        val content = uiState as? DashboardUiState.Content ?: return
        uiState = content.copy(editor = null)
    }

    fun clearMessage() {
        val content = uiState as? DashboardUiState.Content ?: return
        uiState = content.copy(message = null, errorMessage = null)
    }

    private suspend fun resolvePlayersForDashboard(): Result<List<DashboardPlayerUiModel>> = runCatching {
        (playerRepository?.getPlayers() ?: Result.success(emptyList()))
            .getOrThrow()
            .map { DashboardPlayerUiModel(id = it.id, name = it.name) }
    }

    private fun UpcomingGameNight.toUiModel(): GameNightUiModel = GameNightUiModel(
        id = gameNight.id,
        date = gameNight.startsAt.format(dateFormatter),
        time = gameNight.startsAt.format(timeFormatter),
        hostName = host.name,
        hostId = host.id,
        location = gameNight.location,
        startsAt = gameNight.startsAt,
    )

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

        fun factory(
            repository: BoardGamerRepository,
            currentPlayerId: Long,
            onSendNotification: ((title: String, message: String) -> Unit)? = null,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
                    return DashboardViewModel(
                        repository = repository,
                        playerRepository = repository,
                        lateNoticeRepository = repository,
                        currentPlayerId = currentPlayerId,
                        onSendNotification = onSendNotification,
                    ) as T
                }
            }
    }
}
