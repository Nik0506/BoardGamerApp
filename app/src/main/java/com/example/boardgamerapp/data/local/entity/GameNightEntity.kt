package com.example.boardgamerapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.boardgamerapp.domain.model.GameNightStatus
import java.time.LocalDateTime

@Entity(
    tableName = "game_nights",
    indices = [
        Index(value = ["startsAt"]),
        Index(value = ["hostId"]),
    ],
)
data class GameNightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startsAt: LocalDateTime,
    val hostId: Long,
    val location: String,
    val status: GameNightStatus,
)
