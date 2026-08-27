package com.example.boardgamerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.boardgamerapp.data.local.entity.GameNightEntity

@Dao
interface GameNightDao {
    @Query("SELECT * FROM game_nights")
    fun getAll(): List<GameNightEntity>

    @Query("SELECT COUNT(*) FROM game_nights")
    fun count(): Int

    @Insert
    fun insert(gameNight: GameNightEntity): Long

    @Update
    fun update(gameNight: GameNightEntity)
}
