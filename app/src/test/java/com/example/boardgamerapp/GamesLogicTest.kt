package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.BoardGameSuggestion
import com.example.boardgamerapp.data.repository.BoardGameVoteResult
import com.example.boardgamerapp.data.repository.GameSuggestionRepository
import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.data.repository.VotingRepository
import com.example.boardgamerapp.data.repository.VotingSnapshot
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Vote
import com.example.boardgamerapp.ui.games.GamesViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GamesLogicTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createPlayerRepo(players: List<Player>): PlayerRepository = object : PlayerRepository {
        override suspend fun getPlayers(): Result<List<Player>> = Result.success(players)
        override suspend fun addPlayer(name: String, address: String): Result<Player> = error("")
        override suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player> = error("")
        override suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> = error("")
        override suspend fun createNextGameNight(startsAt: LocalDateTime?, preferredHostUid: String?, memberOrderOverride: List<String>?): Result<UpcomingGameNight> = error("")
    }

    private fun createVotingRepo(
        snapshot: VotingSnapshot?,
        onCastVote: ((Long, Long) -> Unit)? = null,
    ): VotingRepository = object : VotingRepository {
        override suspend fun getVotingSnapshot(): Result<VotingSnapshot?> = Result.success(snapshot)
        override suspend fun castVote(playerId: Long, boardGameId: Long): Result<Vote> {
            onCastVote?.invoke(playerId, boardGameId)
            return Result.success(Vote(1L, playerId, boardGameId, 42L))
        }
    }

    private fun createGameSuggestionRepo(
        onAddSuggestion: ((String, String, Long) -> Unit)? = null,
    ): GameSuggestionRepository = object : GameSuggestionRepository {
        override suspend fun getGameSuggestions(): Result<com.example.boardgamerapp.data.repository.GameNightSuggestions?> = Result.success(null)
        override suspend fun addGameSuggestion(name: String, description: String, suggestedByPlayerId: Long): Result<BoardGameSuggestion> {
            onAddSuggestion?.invoke(name, description, suggestedByPlayerId)
            val game = BoardGame(99L, name, description, suggestedByPlayerId, 42L)
            val player = Player(suggestedByPlayerId, "User", "", 1)
            return Result.success(BoardGameSuggestion(game, player))
        }
        override suspend fun deleteGameSuggestion(boardGameId: Long, requestingPlayerId: Long): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `games resultText computes empty, single winner and tie correctly`() = runTest(dispatcher) {
        val host = Player(1L, "Max", "Addr", 1)
        val gameNight = GameNight(42L, LocalDateTime.of(2026, 9, 15, 19, 0), 1L, "Addr", GameNightStatus.PLANNED)
        val game1 = BoardGame(10L, "Catan", "", 1L, 42L)
        val game2 = BoardGame(20L, "Carcassonne", "", 1L, 42L)

        val s1 = BoardGameSuggestion(game1, host)
        val s2 = BoardGameSuggestion(game2, host)

        // 1. Empty votes
        val emptySnapshot = VotingSnapshot(
            gameNight = gameNight,
            results = listOf(BoardGameVoteResult(s1, emptySet()), BoardGameVoteResult(s2, emptySet())),
            playerCount = 1,
        )
        val vmEmpty = GamesViewModel(createGameSuggestionRepo(), createPlayerRepo(listOf(host)), createVotingRepo(emptySnapshot), 1L, dispatcher)
        advanceUntilIdle()
        assertEquals("Noch keine Stimmen", vmEmpty.uiState.resultText)

        // 2. Clear winner
        val winnerSnapshot = VotingSnapshot(
            gameNight = gameNight,
            results = listOf(BoardGameVoteResult(s1, setOf(1L)), BoardGameVoteResult(s2, emptySet())),
            playerCount = 1,
        )
        val vmWinner = GamesViewModel(createGameSuggestionRepo(), createPlayerRepo(listOf(host)), createVotingRepo(winnerSnapshot), 1L, dispatcher)
        advanceUntilIdle()
        assertEquals("Aktuell vorne: Catan", vmWinner.uiState.resultText)

        // 3. Tie
        val tieSnapshot = VotingSnapshot(
            gameNight = gameNight,
            results = listOf(BoardGameVoteResult(s1, setOf(1L)), BoardGameVoteResult(s2, setOf(2L))),
            playerCount = 2,
        )
        val vmTie = GamesViewModel(createGameSuggestionRepo(), createPlayerRepo(listOf(host)), createVotingRepo(tieSnapshot), 1L, dispatcher)
        advanceUntilIdle()
        assertEquals("Gleichstand: Catan und Carcassonne", vmTie.uiState.resultText)
    }

    @Test
    fun `suggestions mapping marks isSelected for current user`() = runTest(dispatcher) {
        val host = Player(1L, "Max", "Addr", 1)
        val p2 = Player(2L, "Erika", "Addr2", 2)
        val gameNight = GameNight(42L, LocalDateTime.of(2026, 9, 15, 19, 0), 1L, "Addr", GameNightStatus.PLANNED)

        val game1 = BoardGame(10L, "Terraforming Mars", "Marsbesiedlung", 1L, 42L)
        val game2 = BoardGame(20L, "Wingspan", "Vogelspiel", 2L, 42L)

        val snapshot = VotingSnapshot(
            gameNight = gameNight,
            results = listOf(
                BoardGameVoteResult(BoardGameSuggestion(game1, host), voterIds = setOf(2L)),
                BoardGameVoteResult(BoardGameSuggestion(game2, p2), voterIds = setOf(1L)),
            ),
            playerCount = 2,
        )

        // Current player is 2 (Erika)
        val vm = GamesViewModel(createGameSuggestionRepo(), createPlayerRepo(listOf(host, p2)), createVotingRepo(snapshot), 2L, dispatcher)
        advanceUntilIdle()

        val suggestions = vm.uiState.suggestions
        assertEquals(2, suggestions.size)

        val terraforming = suggestions.first { it.id == 10L }
        assertTrue(terraforming.isSelected) // Erika voted for Terraforming Mars

        val wingspan = suggestions.first { it.id == 20L }
        assertFalse(wingspan.isSelected) // Erika did not vote for Wingspan
    }

    @Test
    fun `beginAddSuggestion requires membership and active game night`() = runTest(dispatcher) {
        val host = Player(1L, "Max", "Addr", 1)
        val gameNight = GameNight(42L, LocalDateTime.of(2026, 9, 15, 19, 0), 1L, "Addr", GameNightStatus.PLANNED)
        val snapshot = VotingSnapshot(gameNight, emptyList(), 1)

        // User is not in players list
        val vmNonMember = GamesViewModel(createGameSuggestionRepo(), createPlayerRepo(listOf(host)), createVotingRepo(snapshot), 999L, dispatcher)
        advanceUntilIdle()
        vmNonMember.beginAddSuggestion()
        assertEquals("Dein Konto ist kein Mitglied der aktiven Gruppe.", vmNonMember.uiState.errorMessage)
        assertNull(vmNonMember.uiState.editor)

        // User is member: editor opens
        val vmMember = GamesViewModel(createGameSuggestionRepo(), createPlayerRepo(listOf(host)), createVotingRepo(snapshot), 1L, dispatcher)
        advanceUntilIdle()
        vmMember.beginAddSuggestion()
        assertNotNull(vmMember.uiState.editor)
        assertNull(vmMember.uiState.errorMessage)
    }

    @Test
    fun `castVote triggers voting repository and message`() = runTest(dispatcher) {
        val host = Player(1L, "Max", "Addr", 1)
        val gameNight = GameNight(42L, LocalDateTime.of(2026, 9, 15, 19, 0), 1L, "Addr", GameNightStatus.PLANNED)
        val snapshot = VotingSnapshot(gameNight, emptyList(), 1)

        var votedGameId: Long? = null
        val vm = GamesViewModel(
            gameRepository = createGameSuggestionRepo(),
            playerRepository = createPlayerRepo(listOf(host)),
            votingRepository = createVotingRepo(snapshot) { _, gameId -> votedGameId = gameId },
            currentPlayerId = 1L,
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        vm.castVote(42L)
        advanceUntilIdle()

        assertEquals(42L, votedGameId)
        assertEquals("Deine Stimme wurde gespeichert.", vm.uiState.message)
    }
}
