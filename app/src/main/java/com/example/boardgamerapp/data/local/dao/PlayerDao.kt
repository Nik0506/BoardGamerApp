package com.example.boardgamerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.boardgamerapp.data.local.entity.PlayerEntity

@Dao
abstract class PlayerDao {
    @Query("SELECT * FROM players ORDER BY hostOrder")
    abstract fun getAll(): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id")
    abstract fun getById(id: Long): PlayerEntity?

    @Query("SELECT COUNT(*) FROM players")
    abstract fun count(): Int

    @Insert
    abstract fun insert(player: PlayerEntity): Long

    @Update
    abstract fun update(player: PlayerEntity)

    @Transaction
    open fun updateHostOrder(players: List<PlayerEntity>) {
        players.forEach { update(it.copy(hostOrder = it.hostOrder + 1_000_000)) }
        players.forEach { update(it) }
    }
}
