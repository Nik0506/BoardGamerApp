package com.example.boardgamerapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "votes",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BoardGameEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardGameId"],
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
        Index(value = ["playerId", "gameNightId"], unique = true),
        Index(value = ["boardGameId"]),
        Index(value = ["gameNightId"]),
    ],
)
data class VoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerId: Long,
    val boardGameId: Long,
    val gameNightId: Long,
)
