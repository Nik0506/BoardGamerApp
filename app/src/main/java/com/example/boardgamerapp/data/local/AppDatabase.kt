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
import com.example.boardgamerapp.data.local.dao.FoodDao
import com.example.boardgamerapp.data.local.dao.LateNoticeDao
import com.example.boardgamerapp.data.local.dao.PlayerDao
import com.example.boardgamerapp.data.local.dao.OrderDao
import com.example.boardgamerapp.data.local.dao.ReviewDao
import com.example.boardgamerapp.data.local.dao.VoteDao
import com.example.boardgamerapp.data.local.entity.BoardGameEntity
import com.example.boardgamerapp.data.local.entity.GameNightEntity
import com.example.boardgamerapp.data.local.entity.FoodCategoryEntity
import com.example.boardgamerapp.data.local.entity.FoodVoteEntity
import com.example.boardgamerapp.data.local.entity.LateNoticeEntity
import com.example.boardgamerapp.data.local.entity.PlayerEntity
import com.example.boardgamerapp.data.local.entity.ReviewEntity
import com.example.boardgamerapp.data.local.entity.VoteEntity
import com.example.boardgamerapp.data.local.entity.RestaurantEntity
import com.example.boardgamerapp.data.local.entity.FoodOrderEntity

@Database(
    entities = [
        PlayerEntity::class,
        GameNightEntity::class,
        BoardGameEntity::class,
        VoteEntity::class,
        LateNoticeEntity::class,
        ReviewEntity::class,
        FoodCategoryEntity::class,
        FoodVoteEntity::class,
        RestaurantEntity::class,
        FoodOrderEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao

    abstract fun gameNightDao(): GameNightDao

    abstract fun boardGameDao(): BoardGameDao

    abstract fun voteDao(): VoteDao

    abstract fun lateNoticeDao(): LateNoticeDao

    abstract fun reviewDao(): ReviewDao

    abstract fun foodDao(): FoodDao

    abstract fun orderDao(): OrderDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reviews` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `playerId` INTEGER NOT NULL,
                        `gameNightId` INTEGER NOT NULL,
                        `hostRating` INTEGER NOT NULL,
                        `foodRating` INTEGER NOT NULL,
                        `eveningRating` INTEGER NOT NULL,
                        `comment` TEXT NOT NULL,
                        FOREIGN KEY(`playerId`) REFERENCES `players`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`gameNightId`) REFERENCES `game_nights`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        CHECK (`hostRating` BETWEEN 1 AND 5 AND `foodRating` BETWEEN 1 AND 5 AND `eveningRating` BETWEEN 1 AND 5)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_reviews_playerId_gameNightId` ON `reviews` (`playerId`, `gameNightId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reviews_gameNightId` ON `reviews` (`gameNightId`)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `food_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `gameNightId` INTEGER NOT NULL,
                        FOREIGN KEY(`gameNightId`) REFERENCES `game_nights`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_categories_gameNightId` ON `food_categories` (`gameNightId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_categories_gameNightId_name` ON `food_categories` (`gameNightId`, `name`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `food_votes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `playerId` INTEGER NOT NULL,
                        `foodCategoryId` INTEGER NOT NULL,
                        `gameNightId` INTEGER NOT NULL,
                        FOREIGN KEY(`playerId`) REFERENCES `players`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`foodCategoryId`) REFERENCES `food_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`gameNightId`) REFERENCES `game_nights`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_votes_playerId_gameNightId` ON `food_votes` (`playerId`, `gameNightId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_votes_foodCategoryId` ON `food_votes` (`foodCategoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_votes_gameNightId` ON `food_votes` (`gameNightId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `restaurants` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `gameNightId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `menuUrl` TEXT NOT NULL,
                        FOREIGN KEY(`gameNightId`) REFERENCES `game_nights`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_restaurants_gameNightId` ON `restaurants` (`gameNightId`)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `food_orders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `gameNightId` INTEGER NOT NULL,
                        `playerId` INTEGER NOT NULL,
                        `dish` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `priceCents` INTEGER NOT NULL,
                        FOREIGN KEY(`gameNightId`) REFERENCES `game_nights`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`playerId`) REFERENCES `players`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        CHECK (`priceCents` >= 0)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_orders_gameNightId` ON `food_orders` (`gameNightId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_orders_playerId_gameNightId` ON `food_orders` (`playerId`, `gameNightId`)")
            }
        }
    }
}
