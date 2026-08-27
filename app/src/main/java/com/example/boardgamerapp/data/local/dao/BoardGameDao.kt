package com.example.boardgamerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.boardgamerapp.data.local.entity.BoardGameEntity

@Dao
interface BoardGameDao {
    @Query("SELECT * FROM board_games")
    fun getAll(): List<BoardGameEntity>

    @Query("SELECT * FROM board_games WHERE id = :id")
    fun getById(id: Long): BoardGameEntity?

    @Query("SELECT COUNT(*) FROM board_games")
    fun count(): Int

    @Insert
    fun insert(boardGame: BoardGameEntity): Long

    @Delete
    fun delete(boardGame: BoardGameEntity)
}
