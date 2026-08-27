package com.example.boardgamerapp.domain.model

import java.time.LocalDateTime

data class LateNotice(
    val id: Long,
    val playerId: Long,
    val gameNightId: Long,
    val minutes: Int,
    val createdAt: LocalDateTime,
)
