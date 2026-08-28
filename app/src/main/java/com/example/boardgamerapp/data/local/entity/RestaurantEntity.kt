package com.example.boardgamerapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "restaurants",
    foreignKeys = [ForeignKey(entity = GameNightEntity::class, parentColumns = ["id"], childColumns = ["gameNightId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["gameNightId"], unique = true)],
)
data class RestaurantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameNightId: Long,
    val name: String,
    val menuUrl: String,
)
