package com.example.boardgamerapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.boardgamerapp.data.local.dao.BoardGameDao
import com.example.boardgamerapp.data.local.dao.GameNightDao
import com.example.boardgamerapp.data.local.dao.PlayerDao
import com.example.boardgamerapp.data.local.dao.VoteDao
import com.example.boardgamerapp.data.local.entity.BoardGameEntity
import com.example.boardgamerapp.data.local.entity.GameNightEntity
import com.example.boardgamerapp.data.local.entity.PlayerEntity
import com.example.boardgamerapp.data.local.entity.VoteEntity

@Database(
    entities = [
        PlayerEntity::class,
        GameNightEntity::class,
        BoardGameEntity::class,
        VoteEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao

    abstract fun gameNightDao(): GameNightDao

    abstract fun boardGameDao(): BoardGameDao

    abstract fun voteDao(): VoteDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "boardgamer.db",
                )
                    // The existing repository contract is synchronous. A future
                    // coroutine-based API can remove this compatibility setting.
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
    }
}
