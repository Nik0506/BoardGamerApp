package com.example.boardgamerapp.ui.dashboard

import com.example.boardgamerapp.domain.model.AttendanceStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class GameNightUiModel(
    val id: Long = 0L,
    val date: String,
    val time: String,
    val hostName: String,
    val hostId: Long = 0L,
    val location: String,
    val startsAt: LocalDateTime? = null,
    val groupId: String = "",
    val groupName: String = "",
)

data class GameNightPickerUiModel(
    val groupId: String,
    val gameNightDocId: String,
    val groupName: String,
    val date: String,
    val time: String,
    val hostName: String,
    val isSelected: Boolean,
    val hasCollision: Boolean,
)

data class DashboardPlayerUiModel(
    val id: Long,
    val name: String,
)

data class DashboardAttendanceUiModel(
    val playerId: Long,
    val playerName: String,
    val status: AttendanceStatusType,
    val minutesLate: Int? = null,
    val reason: String? = null,
    val updatedAt: String? = null,
    val updatedAtRaw: LocalDateTime? = null,
    val isCurrentPlayer: Boolean = false,
)

enum class StatusReportType {
    LATE,
    DECLINED,
}

data class StatusReportEditorUiState(
    val type: StatusReportType = StatusReportType.LATE,
    val selectedPreset: Int? = 10,
    val customMinutes: String = "",
    val reason: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

data class GameNightEditorUiState(
    val gameNightId: Long = 0L,
    val selectedDate: LocalDate,
    val selectedTime: LocalTime,
    val selectedHostId: Long,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object Empty : DashboardUiState
    data class Content(
        val gameNight: GameNightUiModel,
        val upcomingGameNights: List<GameNightPickerUiModel> = emptyList(),
        val players: List<DashboardPlayerUiModel> = emptyList(),
        val selectedPlayerId: Long? = null,
        val attendances: List<DashboardAttendanceUiModel> = emptyList(),
        val statusReportEditor: StatusReportEditorUiState? = null,
        val gameNightEditor: GameNightEditorUiState? = null,
        val message: String? = null,
        val errorMessage: String? = null,
    ) : DashboardUiState {
        val currentAttendance: DashboardAttendanceUiModel?
            get() = attendances.firstOrNull { it.isCurrentPlayer }
        val attendingCount: Int
            get() = attendances.count { it.status == AttendanceStatusType.ATTENDING }
        val lateCount: Int
            get() = attendances.count { it.status == AttendanceStatusType.LATE }
        val declinedCount: Int
            get() = attendances.count { it.status == AttendanceStatusType.DECLINED }
        val pendingCount: Int
            get() = attendances.count { it.status == AttendanceStatusType.PENDING }
        val recentNotices: List<DashboardAttendanceUiModel>
            get() = attendances
                .filter { it.status == AttendanceStatusType.LATE || it.status == AttendanceStatusType.DECLINED }
                .sortedByDescending { it.updatedAtRaw }
    }
    data class Error(val message: String) : DashboardUiState
}

