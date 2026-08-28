package com.example.boardgamerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.boardgamerapp.data.local.entity.FoodOrderEntity
import com.example.boardgamerapp.data.local.entity.RestaurantEntity

@Dao
abstract class OrderDao {
    @Query("SELECT * FROM restaurants WHERE gameNightId = :gameNightId")
    abstract fun getRestaurant(gameNightId: Long): RestaurantEntity?

    @Insert protected abstract fun insertRestaurant(value: RestaurantEntity): Long
    @Update protected abstract fun updateRestaurant(value: RestaurantEntity)

    @Transaction
    open fun saveRestaurant(value: RestaurantEntity): RestaurantEntity {
        val old = getRestaurant(value.gameNightId)
        return if (old == null) value.copy(id = insertRestaurant(value))
        else value.copy(id = old.id).also(::updateRestaurant)
    }

    @Query("SELECT * FROM food_orders WHERE gameNightId = :gameNightId ORDER BY playerId")
    abstract fun getOrders(gameNightId: Long): List<FoodOrderEntity>

    @Query("SELECT * FROM food_orders WHERE playerId = :playerId AND gameNightId = :gameNightId")
    protected abstract fun getOrder(playerId: Long, gameNightId: Long): FoodOrderEntity?
    @Insert protected abstract fun insertOrder(value: FoodOrderEntity): Long
    @Update protected abstract fun updateOrder(value: FoodOrderEntity)

    @Transaction
    open fun saveOrder(value: FoodOrderEntity): FoodOrderEntity {
        val old = getOrder(value.playerId, value.gameNightId)
        return if (old == null) value.copy(id = insertOrder(value))
        else value.copy(id = old.id).also(::updateOrder)
    }

    @Query("DELETE FROM food_orders WHERE id = :id")
    abstract fun deleteOrder(id: Long)
}
