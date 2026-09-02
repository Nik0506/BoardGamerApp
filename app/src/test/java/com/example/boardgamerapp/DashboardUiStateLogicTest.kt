package com.example.boardgamerapp

import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.example.boardgamerapp.ui.dashboard.DashboardAttendanceUiModel
import com.example.boardgamerapp.ui.dashboard.DashboardPlayerUiModel
import com.example.boardgamerapp.ui.dashboard.DashboardUiState
import com.example.boardgamerapp.ui.dashboard.GameNightPickerUiModel
import com.example.boardgamerapp.ui.dashboard.GameNightUiModel
import com.example.boardgamerapp.ui.dashboard.HostDeclineOption
import com.example.boardgamerapp.ui.dashboard.StatusReportEditorUiState
import com.example.boardgamerapp.ui.dashboard.StatusReportType
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardUiStateLogicTest {

    private val sampleGameNight = GameNightUiModel(
        id = 42L,
        date = "Freitag, 18. September 2026",
        time = "19:00 Uhr",
        hostName = "Max Mustermann",
        hostId = 1L,
        location = "Musterstraße 12",
        groupName = "Würfelfreunde",
    )

    @Test
    fun `isHost is true only when selectedPlayerId matches hostId`() {
        val hostContent = DashboardUiState.Content(
            gameNight = sampleGameNight,
            selectedPlayerId = 1L,
        )
        assertTrue(hostContent.isHost)

        val nonHostContent = DashboardUiState.Content(
            gameNight = sampleGameNight,
            selectedPlayerId = 2L,
        )
        assertFalse(nonHostContent.isHost)

        val unselectedContent = DashboardUiState.Content(
            gameNight = sampleGameNight,
            selectedPlayerId = null,
        )
        assertFalse(unselectedContent.isHost)
    }

    @Test
    fun `currentAttendance resolves item with isCurrentPlayer true`() {
        val a1 = DashboardAttendanceUiModel(
            playerId = 1L,
            playerName = "Max",
            status = AttendanceStatusType.ATTENDING,
            isCurrentPlayer = false,
        )
        val a2 = DashboardAttendanceUiModel(
            playerId = 2L,
            playerName = "Erika",
            status = AttendanceStatusType.LATE,
            minutesLate = 20,
            isCurrentPlayer = true,
        )

        val content = DashboardUiState.Content(
            gameNight = sampleGameNight,
            attendances = listOf(a1, a2),
            selectedPlayerId = 2L,
        )

        assertNotNull(content.currentAttendance)
        assertEquals("Erika", content.currentAttendance?.playerName)
        assertEquals(AttendanceStatusType.LATE, content.currentAttendance?.status)
        assertEquals(20, content.currentAttendance?.minutesLate)
    }

    @Test
    fun `attendance counters correctly aggregate status types`() {
        val attendances = listOf(
            DashboardAttendanceUiModel(playerId = 1L, playerName = "A", status = AttendanceStatusType.ATTENDING),
            DashboardAttendanceUiModel(playerId = 2L, playerName = "B", status = AttendanceStatusType.ATTENDING),
            DashboardAttendanceUiModel(playerId = 3L, playerName = "C", status = AttendanceStatusType.LATE, minutesLate = 10),
            DashboardAttendanceUiModel(playerId = 4L, playerName = "D", status = AttendanceStatusType.DECLINED, reason = "Krank"),
            DashboardAttendanceUiModel(playerId = 5L, playerName = "E", status = AttendanceStatusType.DECLINED),
            DashboardAttendanceUiModel(playerId = 6L, playerName = "F", status = AttendanceStatusType.PENDING),
            DashboardAttendanceUiModel(playerId = 7L, playerName = "G", status = AttendanceStatusType.PENDING),
            DashboardAttendanceUiModel(playerId = 8L, playerName = "H", status = AttendanceStatusType.PENDING),
        )

        val content = DashboardUiState.Content(
            gameNight = sampleGameNight,
            attendances = attendances,
        )

        assertEquals(2, content.attendingCount)
        assertEquals(1, content.lateCount)
        assertEquals(2, content.declinedCount)
        assertEquals(3, content.pendingCount)
    }

    @Test
    fun `recentNotices filters only LATE and DECLINED and sorts by updatedAtRaw descending`() {
        val t1 = LocalDateTime.of(2026, 9, 2, 10, 0)
        val t2 = LocalDateTime.of(2026, 9, 2, 12, 0)
        val t3 = LocalDateTime.of(2026, 9, 2, 14, 0)
        val t4 = LocalDateTime.of(2026, 9, 2, 16, 0)

        val attendances = listOf(
            DashboardAttendanceUiModel(playerId = 1L, playerName = "Attending User", status = AttendanceStatusType.ATTENDING, updatedAtRaw = t4),
            DashboardAttendanceUiModel(playerId = 2L, playerName = "Early Decline", status = AttendanceStatusType.DECLINED, updatedAtRaw = t1),
            DashboardAttendanceUiModel(playerId = 3L, playerName = "Pending User", status = AttendanceStatusType.PENDING, updatedAtRaw = t3),
            DashboardAttendanceUiModel(playerId = 4L, playerName = "Latest Late Notice", status = AttendanceStatusType.LATE, minutesLate = 30, updatedAtRaw = t4),
            DashboardAttendanceUiModel(playerId = 5L, playerName = "Middle Decline", status = AttendanceStatusType.DECLINED, updatedAtRaw = t2),
        )

        val content = DashboardUiState.Content(
            gameNight = sampleGameNight,
            attendances = attendances,
        )

        val notices = content.recentNotices
        assertEquals(3, notices.size)
        // Should be ordered descending: t4 (Latest Late Notice), t2 (Middle Decline), t1 (Early Decline)
        assertEquals("Latest Late Notice", notices[0].playerName)
        assertEquals("Middle Decline", notices[1].playerName)
        assertEquals("Early Decline", notices[2].playerName)
    }

    @Test
    fun `StatusReportEditorUiState initializes with valid defaults`() {
        val editor = StatusReportEditorUiState()
        assertEquals(StatusReportType.LATE, editor.type)
        assertEquals(10, editor.selectedPreset)
        assertEquals("", editor.customMinutes)
        assertEquals("", editor.reason)
        assertEquals(HostDeclineOption.REASSIGN_HOST, editor.hostDeclineOption)
        assertEquals(0L, editor.selectedNewHostId)
        assertFalse(editor.isSaving)
        assertNull(editor.errorMessage)
    }
}
