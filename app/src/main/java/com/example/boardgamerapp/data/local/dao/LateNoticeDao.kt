package com.example.boardgamerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.boardgamerapp.data.local.entity.LateNoticeEntity

@Dao
interface LateNoticeDao {
    @Query("SELECT * FROM late_notices ORDER BY createdAt DESC")
    fun getAll(): List<LateNoticeEntity>

    @Query("SELECT * FROM late_notices WHERE gameNightId = :gameNightId ORDER BY createdAt DESC")
    fun getForGameNight(gameNightId: Long): List<LateNoticeEntity>

    @Query("SELECT COUNT(*) FROM late_notices")
    fun count(): Int

    @Insert
    fun insert(lateNotice: LateNoticeEntity): Long
}
