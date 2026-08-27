package com.example.boardgamerapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "board_games",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["suggestedByPlayerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = GameNightEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameNightId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["suggestedByPlayerId"]),
        Index(value = ["gameNightId"]),
    ],
)
data class BoardGameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val suggestedByPlayerId: Long,
    val gameNightId: Long,
)
