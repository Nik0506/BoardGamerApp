package com.example.boardgamerapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.example.boardgamerapp.data.local.dao.BoardGameDao
import com.example.boardgamerapp.data.local.dao.GameNightDao
import com.example.boardgamerapp.data.local.dao.LateNoticeDao
import com.example.boardgamerapp.data.local.dao.PlayerDao
import com.example.boardgamerapp.data.local.dao.VoteDao
import com.example.boardgamerapp.data.local.entity.BoardGameEntity
import com.example.boardgamerapp.data.local.entity.GameNightEntity
import com.example.boardgamerapp.data.local.entity.LateNoticeEntity
import com.example.boardgamerapp.data.local.entity.PlayerEntity
import com.example.boardgamerapp.data.local.entity.VoteEntity

@Database(
    entities = [
        PlayerEntity::class,
        GameNightEntity::class,
        BoardGameEntity::class,
        VoteEntity::class,
        LateNoticeEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao

    abstract fun gameNightDao(): GameNightDao

    abstract fun boardGameDao(): BoardGameDao

    abstract fun voteDao(): VoteDao

    abstract fun lateNoticeDao(): LateNoticeDao

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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `late_notices` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `playerId` INTEGER NOT NULL,
                        `gameNightId` INTEGER NOT NULL,
                        `minutes` INTEGER NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        FOREIGN KEY(`playerId`) REFERENCES `players`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`gameNightId`) REFERENCES `game_nights`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_late_notices_playerId` ON `late_notices` (`playerId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_late_notices_gameNightId` ON `late_notices` (`gameNightId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_late_notices_createdAt` ON `late_notices` (`createdAt`)",
                )
            }
        }
    }
}
