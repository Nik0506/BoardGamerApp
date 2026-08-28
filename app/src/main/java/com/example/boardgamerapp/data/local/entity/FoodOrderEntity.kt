package com.example.boardgamerapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_orders",
    foreignKeys = [
        ForeignKey(entity = GameNightEntity::class, parentColumns = ["id"], childColumns = ["gameNightId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlayerEntity::class, parentColumns = ["id"], childColumns = ["playerId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["gameNightId"]), Index(value = ["playerId", "gameNightId"], unique = true)],
)
data class FoodOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameNightId: Long,
    val playerId: Long,
    val dish: String,
    val note: String,
    val priceCents: Long,
)
