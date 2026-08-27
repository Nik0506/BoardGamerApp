package com.example.boardgamerapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "late_notices",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GameNightEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameNightId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["playerId"]),
        Index(value = ["gameNightId"]),
        Index(value = ["createdAt"]),
    ],
)
data class LateNoticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerId: Long,
    val gameNightId: Long,
    val minutes: Int,
    val createdAt: LocalDateTime,
)
