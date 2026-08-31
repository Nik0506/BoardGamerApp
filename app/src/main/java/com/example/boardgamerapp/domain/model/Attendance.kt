package com.example.boardgamerapp.domain.model

import java.time.LocalDateTime

enum class AttendanceStatusType {
    ATTENDING,
    LATE,
    DECLINED,
    PENDING,
}

data class GameNightAttendance(
    val id: Long,
    val playerId: Long,
    val gameNightId: Long,
    val status: AttendanceStatusType,
    val minutesLate: Int? = null,
    val reason: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
