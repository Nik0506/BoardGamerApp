package com.example.boardgamerapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_categories",
    foreignKeys = [
        ForeignKey(
            entity = GameNightEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameNightId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["gameNightId"]),
        Index(value = ["gameNightId", "name"], unique = true),
    ],
)
data class FoodCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gameNightId: Long,
)
