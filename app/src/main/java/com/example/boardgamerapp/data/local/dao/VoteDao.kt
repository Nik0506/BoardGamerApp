package com.example.boardgamerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.boardgamerapp.data.local.entity.VoteEntity

@Dao
abstract class VoteDao {
    @Query("SELECT * FROM votes")
    abstract fun getAll(): List<VoteEntity>

    @Query("SELECT * FROM votes WHERE gameNightId = :gameNightId")
    abstract fun getForGameNight(gameNightId: Long): List<VoteEntity>

    @Query("SELECT * FROM votes WHERE playerId = :playerId AND gameNightId = :gameNightId")
    abstract fun getForPlayerAndGameNight(playerId: Long, gameNightId: Long): VoteEntity?

    @Query("SELECT COUNT(*) FROM votes")
    abstract fun count(): Int

    @Insert
    abstract fun insert(vote: VoteEntity): Long

    @Update
    abstract fun update(vote: VoteEntity)

    @Transaction
    open fun replaceForPlayerAndGameNight(vote: VoteEntity): VoteEntity {
        val existing = getForPlayerAndGameNight(vote.playerId, vote.gameNightId)
        return if (existing == null) {
            vote.copy(id = insert(vote))
        } else {
            val replacement = vote.copy(id = existing.id)
            update(replacement)
            replacement
        }
    }
}
