package com.example.boardgamerapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reviews",
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
        Index(value = ["playerId", "gameNightId"], unique = true),
        Index(value = ["gameNightId"]),
    ],
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerId: Long,
    val gameNightId: Long,
    val hostRating: Int,
    val foodRating: Int,
    val eveningRating: Int,
    val comment: String,
)
