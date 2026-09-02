package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.GameNightSuggestions
import com.example.boardgamerapp.data.repository.GameSuggestionRepository
import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.data.repository.VotingRepository
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Vote
import com.example.boardgamerapp.fake.FakeBoardGamerRepository
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
class GamesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeBoardGamerRepository

    private val player1 = Player(1L, "Max Mustermann", "Musterstraße 12", 1)
    private val player2 = Player(2L, "Erika Musterfrau", "Neustraße 5", 2)
    private val sampleNight = GameNight(42L, LocalDateTime.of(2026, 9, 25, 19, 0), player1.id, player1.address, GameNightStatus.PLANNED)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeBoardGamerRepository()
        repository.players.addAll(listOf(player1, player2))
        repository.gameNights.add(sampleNight)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load populates players, date and suggestions correctly`() = runTest(dispatcher) {
        val suggestion = repository.addGameSuggestion("Catan", "Klassiker", player1.id).getOrThrow()
        repository.castVote(player2.id, suggestion.boardGame.id).getOrThrow()

        val vm = GamesViewModel(repository, repository, repository, player2.id, dispatcher)
        advanceUntilIdle()

        assertFalse(vm.uiState.isLoading)
        assertEquals(2, vm.uiState.players.size)
        assertEquals(player2.id, vm.uiState.selectedPlayerId)
        assertEquals("Freitag, 25. September 2026", vm.uiState.gameNightDate)
        assertEquals(1, vm.uiState.suggestions.size)
        assertEquals(1, vm.uiState.totalVotes)
        assertTrue(vm.uiState.suggestions[0].isSelected) // Player 2 voted for it
        assertEquals("Aktuell vorne: Catan", vm.uiState.resultText)
    }

    @Test
    fun `loadGames sets errorMessage when playerRepository fails`() = runTest(dispatcher) {
        val failingPlayerRepo = object : PlayerRepository {
            override suspend fun getPlayers(): Result<List<Player>> = Result.failure(RuntimeException("Netzwerkfehler"))
            override suspend fun addPlayer(name: String, address: String) = error("")
            override suspend fun updatePlayer(id: Long, name: String, address: String) = error("")
            override suspend fun movePlayer(id: Long, direction: MoveDirection) = error("")
            override suspend fun createNextGameNight(startsAt: LocalDateTime?, preferredHostUid: String?, memberOrderOverride: List<String>?) = error("")
        }

        val vm = GamesViewModel(repository, failingPlayerRepo, repository, player1.id, dispatcher)
        advanceUntilIdle()

        assertFalse(vm.uiState.isLoading)
        assertEquals("Netzwerkfehler", vm.uiState.errorMessage)
    }

    @Test
    fun `castVote stores vote, shows success message and updates tally`() = runTest(dispatcher) {
        val suggestion = repository.addGameSuggestion("Carcassonne", "Legespiel", player1.id).getOrThrow()

        val vm = GamesViewModel(repository, repository, repository, player1.id, dispatcher)
        advanceUntilIdle()
        assertFalse(vm.uiState.suggestions[0].isSelected)

        vm.castVote(suggestion.boardGame.id)
        advanceUntilIdle()

        assertEquals("Deine Stimme wurde gespeichert.", vm.uiState.message)
        assertTrue(vm.uiState.suggestions[0].isSelected)
        assertEquals(1, vm.uiState.totalVotes)
    }

    @Test
    fun `beginAddSuggestion prevents non-members or when no game night planned`() = runTest(dispatcher) {
        // Non-member
        val vmNonMember = GamesViewModel(repository, repository, repository, 9999L, dispatcher)
        advanceUntilIdle()
        vmNonMember.beginAddSuggestion()
        assertEquals("Dein Konto ist kein Mitglied der aktiven Gruppe.", vmNonMember.uiState.errorMessage)
        assertNull(vmNonMember.uiState.editor)

        // Member, but no game night
        repository.gameNights.clear()
        val vmNoNight = GamesViewModel(repository, repository, repository, player1.id, dispatcher)
        advanceUntilIdle()
        vmNoNight.beginAddSuggestion()
        assertEquals("Lege zuerst einen kommenden Spieleabend an.", vmNoNight.uiState.errorMessage)
        assertNull(vmNoNight.uiState.editor)
    }

    @Test
    fun `add suggestion editor lifecycle works from input to save`() = runTest(dispatcher) {
        val vm = GamesViewModel(repository, repository, repository, player1.id, dispatcher)
        advanceUntilIdle()

        vm.beginAddSuggestion()
        assertNotNull(vm.uiState.editor)

        vm.updateEditorName("7 Wonders")
        vm.updateEditorDescription("Kartenspiel")
        assertEquals("7 Wonders", vm.uiState.editor?.name)
        assertEquals("Kartenspiel", vm.uiState.editor?.description)

        vm.saveSuggestion()
        advanceUntilIdle()

        assertNull(vm.uiState.editor)
        assertEquals("7 Wonders wurde vorgeschlagen.", vm.uiState.message)
        assertEquals(1, vm.uiState.suggestions.size)
        assertEquals("7 Wonders", vm.uiState.suggestions[0].name)
    }

    @Test
    fun `dismissEditor closes suggestion editor without saving`() = runTest(dispatcher) {
        val vm = GamesViewModel(repository, repository, repository, player1.id, dispatcher)
        advanceUntilIdle()

        vm.beginAddSuggestion()
        assertNotNull(vm.uiState.editor)

        vm.dismissEditor()
        assertNull(vm.uiState.editor)
    }

    @Test
    fun `deleteSuggestion removes game and shows success message`() = runTest(dispatcher) {
        val suggestion = repository.addGameSuggestion("Dixit", "", player1.id).getOrThrow()

        val vm = GamesViewModel(repository, repository, repository, player1.id, dispatcher)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.suggestions.size)

        vm.deleteSuggestion(suggestion.boardGame.id)
        advanceUntilIdle()

        assertEquals("Der Spielvorschlag wurde gelöscht.", vm.uiState.message)
        assertEquals(0, vm.uiState.suggestions.size)
    }

    @Test
    fun `deleteSuggestion shows error when deletion fails`() = runTest(dispatcher) {
        val suggestion = repository.addGameSuggestion("Dixit", "", player1.id).getOrThrow()

        // Player 2 is not the owner
        val vmGuest = GamesViewModel(repository, repository, repository, player2.id, dispatcher)
        advanceUntilIdle()

        vmGuest.deleteSuggestion(suggestion.boardGame.id)
        advanceUntilIdle()

        assertEquals("Nur der Ersteller kann den Vorschlag löschen.", vmGuest.uiState.errorMessage)
    }

    @Test
    fun `resultText displays empty, single leader and tie correctly`() = runTest(dispatcher) {
        val g1 = repository.addGameSuggestion("Spiel A", "", player1.id).getOrThrow()
        val g2 = repository.addGameSuggestion("Spiel B", "", player1.id).getOrThrow()

        val vm = GamesViewModel(repository, repository, repository, player1.id, dispatcher)
        advanceUntilIdle()
        assertEquals("Noch keine Stimmen", vm.uiState.resultText)

        // Single winner
        repository.castVote(player1.id, g1.boardGame.id).getOrThrow()
        vm.loadGames()
        advanceUntilIdle()
        assertEquals("Aktuell vorne: Spiel A", vm.uiState.resultText)

        // Tie
        repository.castVote(player2.id, g2.boardGame.id).getOrThrow()
        vm.loadGames()
        advanceUntilIdle()
        assertEquals("Gleichstand: Spiel A und Spiel B", vm.uiState.resultText)
    }

    @Test
    fun `clearMessage resets both message and errorMessage`() = runTest(dispatcher) {
        val vm = GamesViewModel(repository, repository, repository, player1.id, dispatcher)
        advanceUntilIdle()

        vm.castVote(999L) // Non-existent game
        advanceUntilIdle()

        assertNotNull(vm.uiState.message)
        vm.clearMessage()
        assertNull(vm.uiState.message)
        assertNull(vm.uiState.errorMessage)
    }

    @Test
    fun `GamesViewModel factory instantiates ViewModel`() {
        val factory = GamesViewModel.factory(repository, repository, repository, player1.id)
        val vm = factory.create(GamesViewModel::class.java)
        assertNotNull(vm)
    }
}
