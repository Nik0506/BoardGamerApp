package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.AttendanceRepository
import com.example.boardgamerapp.data.repository.GameNightRepository
import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.data.repository.UpcomingGameNightSummary
import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.example.boardgamerapp.domain.model.GameNightAttendance
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.ui.dashboard.DashboardUiState
import com.example.boardgamerapp.ui.dashboard.DashboardViewModel
import com.example.boardgamerapp.ui.dashboard.StatusReportType
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `maps repository data to content state`() = runTest(dispatcher) {
        val host = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val gameNight = GameNight(
            id = 1,
            startsAt = LocalDateTime.of(2026, 8, 28, 19, 0),
            hostId = host.id,
            location = "Musterstraße 12, 33100 Paderborn",
            status = GameNightStatus.PLANNED,
        )
        val viewModel = DashboardViewModel(
            repository = repositoryReturning(Result.success(UpcomingGameNight(gameNight, host))),
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        val state = viewModel.uiState as DashboardUiState.Content

        assertEquals("Freitag, 28. August 2026", state.gameNight.date)
        assertEquals("19:00 Uhr", state.gameNight.time)
        assertEquals("Max Mustermann", state.gameNight.hostName)
    }

    @Test
    fun `maps missing game night to empty state`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(repositoryReturning(Result.success(null)), ioDispatcher = dispatcher)
        advanceUntilIdle()

        assertEquals(DashboardUiState.Empty, viewModel.uiState)
    }

    @Test
    fun `maps repository failure to error state`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(
            repositoryReturning(Result.failure(IllegalStateException("Testfehler"))),
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState is DashboardUiState.Error)
        assertEquals("Testfehler", (viewModel.uiState as DashboardUiState.Error).message)
    }

    @Test
    fun `beginEditGameNight initializes editor with current game night values`() = runTest(dispatcher) {
        val host = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val startsAt = LocalDateTime.of(2026, 9, 15, 18, 30)
        val gameNight = GameNight(
            id = 42,
            startsAt = startsAt,
            hostId = host.id,
            location = "Musterstraße 12",
            status = GameNightStatus.PLANNED,
        )
        val playerRepo = object : PlayerRepository {
            override suspend fun getPlayers(): Result<List<Player>> = Result.success(listOf(host))
            override suspend fun addPlayer(name: String, address: String): Result<Player> = error("")
            override suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player> = error("")
            override suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> = error("")
            override suspend fun createNextGameNight(
                startsAt: LocalDateTime?,
                preferredHostUid: String?,
                memberOrderOverride: List<String>?,
            ): Result<UpcomingGameNight> = error("")
        }
        val viewModel = DashboardViewModel(
            repository = repositoryReturning(Result.success(UpcomingGameNight(gameNight, host))),
            playerRepository = playerRepo,
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.beginEditGameNight()

        val content = viewModel.uiState as DashboardUiState.Content
        val editor = content.gameNightEditor
        org.junit.Assert.assertNotNull(editor)
        assertEquals(42L, editor?.gameNightId)
        assertEquals(java.time.LocalDate.of(2026, 9, 15), editor?.selectedDate)
        assertEquals(java.time.LocalTime.of(18, 30), editor?.selectedTime)
        assertEquals(1L, editor?.selectedHostId)
    }

    @Test
    fun `saveEditedGameNight updates repository, triggers notification, and reloads content`() = runTest(dispatcher) {
        val host = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val newHost = Player(2, "Erika Musterfrau", "Neustraße 5", 2)
        var updatedStartsAt: LocalDateTime? = null
        var updatedHostId: Long? = null
        var notificationSent = false

        val startsAt = LocalDateTime.of(2026, 9, 15, 18, 30)
        val gameNight = GameNight(
            id = 42,
            startsAt = startsAt,
            hostId = host.id,
            location = "Musterstraße 12",
            status = GameNightStatus.PLANNED,
        )

        val repo = object : GameNightRepository {
            override suspend fun getUpcomingGameNight(): Result<UpcomingGameNight?> =
                Result.success(UpcomingGameNight(gameNight, host))

            override suspend fun updateGameNight(
                gameNightId: Long,
                startsAt: LocalDateTime,
                hostPlayerId: Long,
            ): Result<UpcomingGameNight> {
                updatedStartsAt = startsAt
                updatedHostId = hostPlayerId
                val updatedNight = gameNight.copy(startsAt = startsAt, hostId = hostPlayerId)
                return Result.success(UpcomingGameNight(updatedNight, newHost))
            }
        }
        val playerRepo = object : PlayerRepository {
            override suspend fun getPlayers(): Result<List<Player>> = Result.success(listOf(host, newHost))
            override suspend fun addPlayer(name: String, address: String): Result<Player> = error("")
            override suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player> = error("")
            override suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> = error("")
            override suspend fun createNextGameNight(
                startsAt: LocalDateTime?,
                preferredHostUid: String?,
                memberOrderOverride: List<String>?,
            ): Result<UpcomingGameNight> = error("")
        }

        val viewModel = DashboardViewModel(
            repository = repo,
            playerRepository = playerRepo,
            onSendNotification = { _, _ -> notificationSent = true },
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.beginEditGameNight()
        val newDate = java.time.LocalDate.of(2026, 10, 2)
        val newTime = java.time.LocalTime.of(20, 0)
        viewModel.updateGameNightEditorDate(newDate)
        viewModel.updateGameNightEditorTime(newTime)
        viewModel.updateGameNightEditorHost(2L)

        viewModel.saveEditedGameNight()
        advanceUntilIdle()

        assertEquals(LocalDateTime.of(2026, 10, 2, 20, 0), updatedStartsAt)
        assertEquals(2L, updatedHostId)
        assertTrue(notificationSent)

        val content = viewModel.uiState as DashboardUiState.Content
        org.junit.Assert.assertNull(content.gameNightEditor)
        assertEquals(
            "Spieleabend wurde erfolgreich aktualisiert. Teilnehmer wurden per Push-Nachricht informiert.",
            content.message,
        )
    }

    @Test
    fun `saveEditedGameNight handles repository failure`() = runTest(dispatcher) {
        val host = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val gameNight = GameNight(
            id = 42,
            startsAt = LocalDateTime.of(2026, 9, 15, 18, 30),
            hostId = host.id,
            location = "Musterstraße 12",
            status = GameNightStatus.PLANNED,
        )

        val repo = object : GameNightRepository {
            override suspend fun getUpcomingGameNight(): Result<UpcomingGameNight?> =
                Result.success(UpcomingGameNight(gameNight, host))

            override suspend fun updateGameNight(
                gameNightId: Long,
                startsAt: LocalDateTime,
                hostPlayerId: Long,
            ): Result<UpcomingGameNight> = Result.failure(RuntimeException("Speichern fehlgeschlagen"))
        }
        val playerRepo = object : PlayerRepository {
            override suspend fun getPlayers(): Result<List<Player>> = Result.success(listOf(host))
            override suspend fun addPlayer(name: String, address: String): Result<Player> = error("")
            override suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player> = error("")
            override suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> = error("")
            override suspend fun createNextGameNight(
                startsAt: LocalDateTime?,
                preferredHostUid: String?,
                memberOrderOverride: List<String>?,
            ): Result<UpcomingGameNight> = error("")
        }

        val viewModel = DashboardViewModel(
            repository = repo,
            playerRepository = playerRepo,
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.beginEditGameNight()
        viewModel.saveEditedGameNight()
        advanceUntilIdle()

        val content = viewModel.uiState as DashboardUiState.Content
        org.junit.Assert.assertNotNull(content.gameNightEditor)
        assertEquals("Speichern fehlgeschlagen", content.gameNightEditor?.errorMessage)
    }

    @Test
    fun `dismissGameNightEditor removes editor from state`() = runTest(dispatcher) {
        val host = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val gameNight = GameNight(
            id = 42,
            startsAt = LocalDateTime.of(2026, 9, 15, 18, 30),
            hostId = host.id,
            location = "Musterstraße 12",
            status = GameNightStatus.PLANNED,
        )
        val viewModel = DashboardViewModel(
            repository = repositoryReturning(Result.success(UpcomingGameNight(gameNight, host))),
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.beginEditGameNight()
        org.junit.Assert.assertNotNull((viewModel.uiState as DashboardUiState.Content).gameNightEditor)

        viewModel.dismissGameNightEditor()
        org.junit.Assert.assertNull((viewModel.uiState as DashboardUiState.Content).gameNightEditor)
    }

    @Test
    fun `loadGameNight handles network offline failure and recovers on retry`() = runTest(dispatcher) {
        var shouldFail = true
        val host = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val gameNight = GameNight(
            id = 42,
            startsAt = LocalDateTime.of(2026, 9, 15, 18, 30),
            hostId = host.id,
            location = "Musterstraße 12",
            status = GameNightStatus.PLANNED,
        )

        val repo = object : GameNightRepository {
            override suspend fun getUpcomingGameNight(): Result<UpcomingGameNight?> =
                if (shouldFail) Result.failure(java.io.IOException("Keine Internetverbindung"))
                else Result.success(UpcomingGameNight(gameNight, host))
        }

        val viewModel = DashboardViewModel(
            repository = repo,
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState is DashboardUiState.Error)
        assertEquals("Keine Internetverbindung", (viewModel.uiState as DashboardUiState.Error).message)

        // Retry when network is back
        shouldFail = false
        viewModel.loadGameNight()
        advanceUntilIdle()

        assertTrue(viewModel.uiState is DashboardUiState.Content)
    }

    @Test
    fun `confirmAttending saves ATTENDING status and triggers notification`() = runTest(dispatcher) {
        val host = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val player2 = Player(2, "Erika Musterfrau", "Neustraße 5", 2)
        val gameNight = GameNight(
            id = 42,
            startsAt = LocalDateTime.of(2026, 9, 15, 18, 30),
            hostId = host.id,
            location = "Musterstraße 12",
            status = GameNightStatus.PLANNED,
        )

        var savedStatus: AttendanceStatusType? = null
        var notificationSentTitle: String? = null
        var notificationSentMessage: String? = null

        val attendances = mutableListOf<GameNightAttendance>()

        val attendanceRepo = object : AttendanceRepository {
            override suspend fun getAttendances(): Result<List<GameNightAttendance>> =
                Result.success(attendances)

            override suspend fun setAttendance(
                playerId: Long,
                status: AttendanceStatusType,
                minutesLate: Int?,
                reason: String?,
            ): Result<GameNightAttendance> {
                savedStatus = status
                val att = GameNightAttendance(
                    id = playerId,
                    playerId = playerId,
                    gameNightId = 42,
                    status = status,
                    minutesLate = minutesLate,
                    reason = reason,
                )
                attendances.removeAll { it.playerId == playerId }
                attendances.add(att)
                return Result.success(att)
            }
        }

        val playerRepo = object : PlayerRepository {
            override suspend fun getPlayers(): Result<List<Player>> = Result.success(listOf(host, player2))
            override suspend fun addPlayer(name: String, address: String): Result<Player> = error("")
            override suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player> = error("")
            override suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> = error("")
            override suspend fun createNextGameNight(
                startsAt: LocalDateTime?,
                preferredHostUid: String?,
                memberOrderOverride: List<String>?,
            ): Result<UpcomingGameNight> = error("")
        }

        val viewModel = DashboardViewModel(
            repository = repositoryReturning(Result.success(UpcomingGameNight(gameNight, host))),
            playerRepository = playerRepo,
            attendanceRepository = attendanceRepo,
            currentPlayerId = 2L,
            onSendNotification = { title, message ->
                notificationSentTitle = title
                notificationSentMessage = message
            },
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.confirmAttending()
        advanceUntilIdle()

        assertEquals(AttendanceStatusType.ATTENDING, savedStatus)
        assertEquals("Status-Update", notificationSentTitle)
        assertTrue(notificationSentMessage?.contains("Erika Musterfrau") == true)
        val content = viewModel.uiState as DashboardUiState.Content
        assertEquals("Deine Zusage wurde gespeichert.", content.message)
        assertEquals(1, content.attendingCount)
    }

    @Test
    fun `saveStatusReport saves DECLINED status with reason and triggers notification`() = runTest(dispatcher) {
        val host = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val gameNight = GameNight(
            id = 42,
            startsAt = LocalDateTime.of(2026, 9, 15, 18, 30),
            hostId = host.id,
            location = "Musterstraße 12",
            status = GameNightStatus.PLANNED,
        )

        var savedReason: String? = null
        var savedStatus: AttendanceStatusType? = null
        var notificationSent = false

        val attendanceRepo = object : AttendanceRepository {
            override suspend fun getAttendances(): Result<List<GameNightAttendance>> = Result.success(emptyList())

            override suspend fun setAttendance(
                playerId: Long,
                status: AttendanceStatusType,
                minutesLate: Int?,
                reason: String?,
            ): Result<GameNightAttendance> {
                savedStatus = status
                savedReason = reason
                return Result.success(
                    GameNightAttendance(
                        id = playerId,
                        playerId = playerId,
                        gameNightId = 42,
                        status = status,
                        reason = reason,
                    ),
                )
            }
        }

        val playerRepo = object : PlayerRepository {
            override suspend fun getPlayers(): Result<List<Player>> = Result.success(listOf(host))
            override suspend fun addPlayer(name: String, address: String): Result<Player> = error("")
            override suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player> = error("")
            override suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> = error("")
            override suspend fun createNextGameNight(
                startsAt: LocalDateTime?,
                preferredHostUid: String?,
                memberOrderOverride: List<String>?,
            ): Result<UpcomingGameNight> = error("")
        }

        val viewModel = DashboardViewModel(
            repository = repositoryReturning(Result.success(UpcomingGameNight(gameNight, host))),
            playerRepository = playerRepo,
            attendanceRepository = attendanceRepo,
            currentPlayerId = 1L,
            onSendNotification = { _, _ -> notificationSent = true },
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.beginStatusReport()
        assertNotNull((viewModel.uiState as DashboardUiState.Content).statusReportEditor)

        viewModel.selectStatusReportType(StatusReportType.DECLINED)
        viewModel.updateDeclineReason("Krank")
        viewModel.saveStatusReport()
        advanceUntilIdle()

        assertEquals(AttendanceStatusType.DECLINED, savedStatus)
        assertEquals("Krank", savedReason)
        assertTrue(notificationSent)
        val content = viewModel.uiState as DashboardUiState.Content
        assertNull(content.statusReportEditor)
        assertEquals("Deine Absage wurde gespeichert.", content.message)
    }

    @Test
    fun `saveStatusReport saves LATE status with minutes and triggers notification`() = runTest(dispatcher) {
        val host = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val gameNight = GameNight(
            id = 42,
            startsAt = LocalDateTime.of(2026, 9, 15, 18, 30),
            hostId = host.id,
            location = "Musterstraße 12",
            status = GameNightStatus.PLANNED,
        )

        var savedStatus: AttendanceStatusType? = null
        var savedMinutes: Int? = null
        var notificationSent = false

        val attendanceRepo = object : AttendanceRepository {
            override suspend fun getAttendances(): Result<List<GameNightAttendance>> = Result.success(emptyList())

            override suspend fun setAttendance(
                playerId: Long,
                status: AttendanceStatusType,
                minutesLate: Int?,
                reason: String?,
            ): Result<GameNightAttendance> {
                savedStatus = status
                savedMinutes = minutesLate
                return Result.success(
                    GameNightAttendance(
                        id = playerId,
                        playerId = playerId,
                        gameNightId = 42,
                        status = status,
                        minutesLate = minutesLate,
                    ),
                )
            }
        }

        val playerRepo = object : PlayerRepository {
            override suspend fun getPlayers(): Result<List<Player>> = Result.success(listOf(host))
            override suspend fun addPlayer(name: String, address: String): Result<Player> = error("")
            override suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player> = error("")
            override suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> = error("")
            override suspend fun createNextGameNight(
                startsAt: LocalDateTime?,
                preferredHostUid: String?,
                memberOrderOverride: List<String>?,
            ): Result<UpcomingGameNight> = error("")
        }

        val viewModel = DashboardViewModel(
            repository = repositoryReturning(Result.success(UpcomingGameNight(gameNight, host))),
            playerRepository = playerRepo,
            attendanceRepository = attendanceRepo,
            currentPlayerId = 1L,
            onSendNotification = { _, _ -> notificationSent = true },
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.beginStatusReport()
        val editorAfterBegin = (viewModel.uiState as DashboardUiState.Content).statusReportEditor
        assertNotNull(editorAfterBegin)
        assertEquals(StatusReportType.LATE, editorAfterBegin?.type)

        viewModel.selectLateNoticePreset(20)
        viewModel.saveStatusReport()
        advanceUntilIdle()

        assertEquals(AttendanceStatusType.LATE, savedStatus)
        assertEquals(20, savedMinutes)
        assertTrue(notificationSent)
        val content = viewModel.uiState as DashboardUiState.Content
        assertNull(content.statusReportEditor)
        assertEquals("Verspätungsmeldung wurde gespeichert.", content.message)
    }

    @Test
    fun `loadGameNight maps upcoming game nights across groups with collision detection`() = runTest(dispatcher) {
        val hostA = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val hostB = Player(2, "Erika Musterfrau", "Neustraße 5", 1)
        val hostC = Player(3, "Tom Beispiel", "Beispielweg 3", 1)
        val collisionTime = LocalDateTime.of(2026, 9, 20, 19, 0)
        val nightA = GameNight(id = 10, startsAt = collisionTime, hostId = hostA.id, location = hostA.address, status = GameNightStatus.PLANNED, groupId = "groupA")
        val nightB = GameNight(id = 11, startsAt = collisionTime, hostId = hostB.id, location = hostB.address, status = GameNightStatus.PLANNED, groupId = "groupB")
        val nightC = GameNight(
            id = 12,
            startsAt = LocalDateTime.of(2026, 9, 27, 19, 0),
            hostId = hostC.id,
            location = hostC.address,
            status = GameNightStatus.PLANNED,
            groupId = "groupC",
        )
        val summaries = listOf(
            UpcomingGameNightSummary("groupA", "Würfelfreunde", "docA", nightA, hostA, isSelected = true),
            UpcomingGameNightSummary("groupB", "Brettspielnacht", "docB", nightB, hostB, isSelected = false),
            UpcomingGameNightSummary("groupC", "Kniffelclub", "docC", nightC, hostC, isSelected = false),
        )

        val repo = object : GameNightRepository {
            override suspend fun getUpcomingGameNight(): Result<UpcomingGameNight?> =
                Result.success(UpcomingGameNight(nightA, hostA))

            override suspend fun getUpcomingGameNights(): Result<List<UpcomingGameNightSummary>> =
                Result.success(summaries)
        }

        val viewModel = DashboardViewModel(repository = repo, ioDispatcher = dispatcher)
        advanceUntilIdle()

        val content = viewModel.uiState as DashboardUiState.Content
        assertEquals(3, content.upcomingGameNights.size)
        assertEquals("Würfelfreunde", content.gameNight.groupName)

        val entryA = content.upcomingGameNights.first { it.groupId == "groupA" }
        val entryB = content.upcomingGameNights.first { it.groupId == "groupB" }
        val entryC = content.upcomingGameNights.first { it.groupId == "groupC" }
        assertTrue(entryA.isSelected)
        assertTrue(entryA.hasCollision)
        assertTrue(entryB.hasCollision)
        assertTrue(!entryC.isSelected)
        assertTrue(!entryC.hasCollision)
    }

    @Test
    fun `selectGameNight switches repository selection and reloads content`() = runTest(dispatcher) {
        val hostA = Player(1, "Max Mustermann", "Musterstraße 12", 1)
        val hostB = Player(2, "Erika Musterfrau", "Neustraße 5", 1)
        val nightA = GameNight(id = 10, startsAt = LocalDateTime.of(2026, 9, 20, 19, 0), hostId = hostA.id, location = hostA.address, status = GameNightStatus.PLANNED, groupId = "groupA")
        val nightB = GameNight(id = 11, startsAt = LocalDateTime.of(2026, 9, 27, 19, 0), hostId = hostB.id, location = hostB.address, status = GameNightStatus.PLANNED, groupId = "groupB")

        var selectedKey = "groupA" to "docA"
        val selectCalls = mutableListOf<Pair<String, String>>()

        val repo = object : GameNightRepository {
            override suspend fun getUpcomingGameNight(): Result<UpcomingGameNight?> =
                Result.success(if (selectedKey.first == "groupA") UpcomingGameNight(nightA, hostA) else UpcomingGameNight(nightB, hostB))

            override suspend fun getUpcomingGameNights(): Result<List<UpcomingGameNightSummary>> = Result.success(
                listOf(
                    UpcomingGameNightSummary("groupA", "Würfelfreunde", "docA", nightA, hostA, isSelected = selectedKey.first == "groupA"),
                    UpcomingGameNightSummary("groupB", "Brettspielnacht", "docB", nightB, hostB, isSelected = selectedKey.first == "groupB"),
                ),
            )

            override fun selectGameNight(groupId: String, gameNightDocId: String) {
                selectCalls.add(groupId to gameNightDocId)
                selectedKey = groupId to gameNightDocId
            }
        }

        val viewModel = DashboardViewModel(repository = repo, ioDispatcher = dispatcher)
        advanceUntilIdle()
        assertEquals("Würfelfreunde", (viewModel.uiState as DashboardUiState.Content).gameNight.groupName)

        viewModel.selectGameNight("groupB", "docB")
        advanceUntilIdle()

        assertEquals(listOf("groupB" to "docB"), selectCalls)
        val content = viewModel.uiState as DashboardUiState.Content
        assertEquals("Brettspielnacht", content.gameNight.groupName)
        assertTrue(content.upcomingGameNights.first { it.groupId == "groupB" }.isSelected)
    }

    private fun repositoryReturning(
        result: Result<UpcomingGameNight?>,
    ): GameNightRepository = object : GameNightRepository {
        override suspend fun getUpcomingGameNight(): Result<UpcomingGameNight?> = result
    }
}
