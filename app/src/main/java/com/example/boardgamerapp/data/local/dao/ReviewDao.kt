package com.example.boardgamerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.boardgamerapp.data.local.entity.ReviewEntity

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE gameNightId = :gameNightId ORDER BY id")
    fun getForGameNight(gameNightId: Long): List<ReviewEntity>

    @Query("SELECT COUNT(*) FROM reviews")
    fun count(): Int

    @Insert
    fun insert(review: ReviewEntity): Long
}
