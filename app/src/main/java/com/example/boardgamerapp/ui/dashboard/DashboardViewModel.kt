package com.example.boardgamerapp.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.boardgamerapp.data.repository.AttendanceRepository
import com.example.boardgamerapp.data.repository.BoardGamerRepository
import com.example.boardgamerapp.data.repository.GameNightRepository
import com.example.boardgamerapp.data.repository.LateNoticeRepository
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.example.boardgamerapp.domain.model.GameNightAttendance
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
    private val attendanceRepository: AttendanceRepository? = null,
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
        val attendancesResult = withContext(ioDispatcher) { attendanceRepository?.getAttendances() ?: Result.success(emptyList()) }
        val error = playersResult.exceptionOrNull() ?: noticesResult.exceptionOrNull() ?: attendancesResult.exceptionOrNull()
        if (error != null) {
            uiState = DashboardUiState.Error(
                message = error.message ?: "Die Daten für den Spieleabend konnten nicht geladen werden.",
            )
            return
        }

        val players = playersResult.getOrThrow()
            .map { DashboardPlayerUiModel(id = it.id, name = it.name) }
        val attendancesMap = attendancesResult.getOrThrow().associateBy { it.playerId }
        val attendanceUiModels = players.map { player ->
            val att = attendancesMap[player.id]
            DashboardAttendanceUiModel(
                playerId = player.id,
                playerName = player.name,
                status = att?.status ?: AttendanceStatusType.PENDING,
                minutesLate = att?.minutesLate,
                reason = att?.reason,
                updatedAt = att?.updatedAt?.format(noticeDateFormatter),
                isCurrentPlayer = (player.id == currentPlayerId),
            )
        }

        uiState = DashboardUiState.Content(
            gameNight = upcoming.toUiModel(),
            players = players,
            selectedPlayerId = currentPlayerId?.takeIf { id -> players.any { it.id == id } },
            attendances = attendanceUiModels,
            lateNotices = noticesResult.getOrThrow().map { it.toUiModel(players) },
            message = successMessage,
        )
    }

    fun confirmAttending() {
        val content = uiState as? DashboardUiState.Content ?: return
        val playerId = content.selectedPlayerId
        if (playerId == null || attendanceRepository == null) {
            uiState = content.copy(errorMessage = "Dein Konto ist kein Mitglied der aktiven Gruppe.")
            return
        }
        val playerName = content.players.firstOrNull { it.id == playerId }?.name ?: "Ein Mitglied"

        viewModelScope.launch {
            withContext(ioDispatcher) {
                attendanceRepository.setAttendance(playerId, AttendanceStatusType.ATTENDING)
            }.fold(
                onSuccess = {
                    onSendNotification?.invoke(
                        "Status-Update",
                        "$playerName ist beim nächsten Spieleabend dabei.",
                    )
                    fetchGameNightContent(successMessage = "Deine Zusage wurde gespeichert.")
                },
                onFailure = { error ->
                    uiState = content.copy(
                        errorMessage = error.message ?: "Die Zusage konnte nicht gespeichert werden.",
                    )
                },
            )
        }
    }

    fun beginDeclineAttendance() {
        val content = uiState as? DashboardUiState.Content ?: return
        if (content.selectedPlayerId == null) {
            uiState = content.copy(errorMessage = "Dein Konto ist kein Mitglied der aktiven Gruppe.")
            return
        }
        uiState = content.copy(
            declineEditor = AttendanceDeclineEditorUiState(),
            message = null,
            errorMessage = null,
        )
    }

    fun updateDeclineReason(reason: String) {
        val content = uiState as? DashboardUiState.Content ?: return
        uiState = content.copy(
            declineEditor = (content.declineEditor ?: AttendanceDeclineEditorUiState()).copy(
                reason = reason,
                errorMessage = null,
            ),
        )
    }

    fun confirmDeclineAttendance() {
        val content = uiState as? DashboardUiState.Content ?: return
        val editor = content.declineEditor ?: return
        val playerId = content.selectedPlayerId
        if (playerId == null || attendanceRepository == null) {
            uiState = content.copy(errorMessage = "Dein Konto ist kein Mitglied der aktiven Gruppe.")
            return
        }
        val playerName = content.players.firstOrNull { it.id == playerId }?.name ?: "Ein Mitglied"
        uiState = content.copy(declineEditor = editor.copy(isSaving = true, errorMessage = null))

        viewModelScope.launch {
            withContext(ioDispatcher) {
                attendanceRepository.setAttendance(
                    playerId = playerId,
                    status = AttendanceStatusType.DECLINED,
                    reason = editor.reason.ifBlank { null },
                )
            }.fold(
                onSuccess = {
                    val reasonSuffix = if (editor.reason.isNotBlank()) " Grund: ${editor.reason}" else ""
                    onSendNotification?.invoke(
                        "Absage zum Spieleabend",
                        "$playerName hat für den Spieleabend abgesagt.$reasonSuffix",
                    )
                    fetchGameNightContent(successMessage = "Deine Absage wurde gespeichert.")
                },
                onFailure = { error ->
                    uiState = content.copy(
                        declineEditor = editor.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Die Absage konnte nicht gespeichert werden.",
                        ),
                    )
                },
            )
        }
    }

    fun dismissDeclineAttendance() {
        val content = uiState as? DashboardUiState.Content ?: return
        uiState = content.copy(declineEditor = null)
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
        if (playerId == null || (lateNoticeRepository == null && attendanceRepository == null)) {
            uiState = content.copy(errorMessage = "Dein Konto ist kein Mitglied der aktiven Gruppe.")
            return
        }
        val playerName = content.players.firstOrNull { it.id == playerId }?.name ?: "Ein Mitglied"

        viewModelScope.launch {
            withContext(ioDispatcher) {
                if (lateNoticeRepository != null) {
                    lateNoticeRepository.addLateNotice(playerId, minutes)
                } else {
                    attendanceRepository?.setAttendance(playerId, AttendanceStatusType.LATE, minutesLate = minutes)
                        ?: Result.failure(IllegalStateException("Repository nicht verfügbar"))
                }
            }.fold(
                onSuccess = {
                    onSendNotification?.invoke(
                        "Verspätung gemeldet",
                        "$playerName verspätet sich um $minutes Minuten.",
                    )
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
                        attendanceRepository = repository,
                        currentPlayerId = currentPlayerId,
                        onSendNotification = onSendNotification,
                    ) as T
                }
            }
    }
}
