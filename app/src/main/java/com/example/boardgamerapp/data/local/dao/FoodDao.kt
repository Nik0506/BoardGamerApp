package com.example.boardgamerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.boardgamerapp.data.local.entity.FoodCategoryEntity
import com.example.boardgamerapp.data.local.entity.FoodVoteEntity

@Dao
abstract class FoodDao {
    @Query("SELECT * FROM food_categories WHERE gameNightId = :gameNightId ORDER BY name COLLATE NOCASE")
    abstract fun getCategories(gameNightId: Long): List<FoodCategoryEntity>

    @Insert
    abstract fun insertCategory(category: FoodCategoryEntity): Long

    @Insert
    abstract fun insertCategories(categories: List<FoodCategoryEntity>): List<Long>

    @Delete
    abstract fun deleteCategory(category: FoodCategoryEntity)

    @Query("SELECT * FROM food_votes WHERE gameNightId = :gameNightId")
    abstract fun getVotes(gameNightId: Long): List<FoodVoteEntity>

    @Query("SELECT * FROM food_votes WHERE playerId = :playerId AND gameNightId = :gameNightId")
    protected abstract fun getVote(playerId: Long, gameNightId: Long): FoodVoteEntity?

    @Insert
    protected abstract fun insertVote(vote: FoodVoteEntity): Long

    @Update
    protected abstract fun updateVote(vote: FoodVoteEntity)

    @Transaction
    open fun replaceVote(vote: FoodVoteEntity): FoodVoteEntity {
        val existing = getVote(vote.playerId, vote.gameNightId)
        return if (existing == null) {
            vote.copy(id = insertVote(vote))
        } else {
            vote.copy(id = existing.id).also(::updateVote)
        }
    }
}
