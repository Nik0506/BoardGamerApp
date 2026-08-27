package com.example.boardgamerapp

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.boardgamerapp.data.local.AppDatabase
import com.example.boardgamerapp.data.local.entity.BoardGameEntity
import com.example.boardgamerapp.data.local.entity.GameNightEntity
import com.example.boardgamerapp.data.local.entity.PlayerEntity
import com.example.boardgamerapp.data.local.entity.VoteEntity
import com.example.boardgamerapp.data.repository.RoomGameNightRepository
import com.example.boardgamerapp.domain.model.GameNightStatus
import java.time.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomGameNightRepositoryTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun repositoryPersistsPlayersAndSuggestionsInDatabase() {
        val repository = RoomGameNightRepository(database, now = { LocalDateTime.of(2026, 8, 27, 12, 0) })
        val player = repository.addPlayer("Max", "Adresse 1").getOrThrow()
        repository.createNextGameNight().getOrThrow()
        repository.addGameSuggestion("Catan", "Handel", player.id).getOrThrow()

        assertEquals(listOf(player), repository.getPlayers().getOrThrow())
        assertEquals("Catan", repository.getGameSuggestions().getOrThrow()?.suggestions?.single()?.boardGame?.name)
    }

    @Test
    fun repositoryPersistsLateNoticeAndAssociatesItWithUpcomingNight() {
        val repository = RoomGameNightRepository(
            database,
            now = { LocalDateTime.of(2026, 8, 27, 12, 0) },
        )
        val player = repository.addPlayer("Max", "Adresse 1").getOrThrow()
        repository.createNextGameNight().getOrThrow()

        val notice = repository.addLateNotice(player.id, 30).getOrThrow()

        assertEquals(30, notice.minutes)
        assertEquals(player.id, notice.playerId)
        assertEquals(listOf(notice), repository.getLateNotices().getOrThrow())
        assertEquals(1, database.lateNoticeDao().count())
    }

    @Test
    fun roomRepositoryRejectsNonPositiveLateNotice() {
        val repository = RoomGameNightRepository(
            database,
            now = { LocalDateTime.of(2026, 8, 27, 12, 0) },
        )
        val player = repository.addPlayer("Max", "Adresse 1").getOrThrow()
        repository.createNextGameNight().getOrThrow()

        assertTrue(repository.addLateNotice(player.id, 0).isFailure)
        assertEquals(0, database.lateNoticeDao().count())
    }

    @Test
    fun voteUniqueIndexRejectsSecondVoteForSamePlayerAndNight() {
        database.playerDao().insert(PlayerEntity(id = 1, name = "Max", address = "Adresse", hostOrder = 1))
        database.gameNightDao().insert(
            GameNightEntity(
                id = 1,
                startsAt = LocalDateTime.of(2026, 8, 28, 19, 0),
                hostId = 1,
                location = "Adresse",
                status = GameNightStatus.PLANNED,
            ),
        )
        database.boardGameDao().insert(
            BoardGameEntity(
                id = 1,
                name = "Catan",
                description = "",
                suggestedByPlayerId = 1,
                gameNightId = 1,
            ),
        )
        database.boardGameDao().insert(
            BoardGameEntity(
                id = 2,
                name = "Heat",
                description = "",
                suggestedByPlayerId = 1,
                gameNightId = 1,
            ),
        )
        val vote = VoteEntity(playerId = 1, boardGameId = 1, gameNightId = 1)
        database.voteDao().insert(vote)

        assertThrows(SQLiteConstraintException::class.java) {
            database.voteDao().insert(vote.copy(boardGameId = 2))
        }
    }

    @Test
    fun schemaHasVersionTwoAndAllIterationSixTables() {
        val sqliteDatabase = database.openHelper.writableDatabase
        assertEquals(2, sqliteDatabase.version)
        val tables = sqliteDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table'",
        ).use { cursor ->
            buildSet {
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }

        assertTrue("players" in tables)
        assertTrue("game_nights" in tables)
        assertTrue("board_games" in tables)
        assertTrue("votes" in tables)
        assertTrue("late_notices" in tables)
    }
}
