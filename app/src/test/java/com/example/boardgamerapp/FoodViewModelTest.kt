package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.FoodVoteResult
import com.example.boardgamerapp.data.repository.FoodVotingSnapshot
import com.example.boardgamerapp.data.repository.OrderingSnapshot
import com.example.boardgamerapp.data.repository.OrderWithPlayer
import com.example.boardgamerapp.domain.model.FoodCategory
import com.example.boardgamerapp.domain.model.FoodOrder
import com.example.boardgamerapp.domain.model.FoodVote
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Restaurant
import com.example.boardgamerapp.fake.FakeBoardGamerRepository
import com.example.boardgamerapp.ui.food.FoodViewModel
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
class FoodViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeBoardGamerRepository

    private val host = Player(1L, "Max Host", "Musterstraße 1", 1)
    private val guest = Player(2L, "Erika Gast", "Neustraße 2", 2)
    private val sampleNight = GameNight(10L, LocalDateTime.of(2026, 9, 20, 19, 0), host.id, host.address, GameNightStatus.PLANNED)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeBoardGamerRepository()
        repository.players.addAll(listOf(host, guest))
        repository.gameNights.add(sampleNight)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load loads voting and ordering snapshot successfully`() = runTest(dispatcher) {
        val cat = repository.addFoodCategory("Pizza").getOrThrow()
        repository.saveRestaurant(host.id, "Pizzeria Bella", "https://menu.com").getOrThrow()
        repository.saveFoodOrder(guest.id, "Pizza Margherita", "Extra Käse", 950L).getOrThrow()

        val vm = FoodViewModel(repository, guest.id, dispatcher)
        advanceUntilIdle()

        assertFalse(vm.uiState.isLoading)
        assertEquals("Pizzeria Bella", vm.uiState.restaurantName)
        assertEquals("https://menu.com", vm.uiState.menuUrl)
        assertEquals(1, vm.uiState.categories.size)
        assertEquals(1, vm.uiState.orders.size)
        assertEquals("9,50 €", vm.uiState.totalPrice)
        assertEquals(guest.id, vm.uiState.selectedPlayerId)
    }

    @Test
    fun `load handles null snapshot gracefully`() = runTest(dispatcher) {
        repository.gameNights.clear() // No game night
        val vm = FoodViewModel(repository, guest.id, dispatcher)
        advanceUntilIdle()

        assertFalse(vm.uiState.isLoading)
        assertTrue(vm.uiState.categories.isEmpty())
        assertTrue(vm.uiState.orders.isEmpty())
    }

    @Test
    fun `voting result text handles empty, single leader and tie`() = runTest(dispatcher) {
        val cat1 = repository.addFoodCategory("Burger").getOrThrow()
        val cat2 = repository.addFoodCategory("Pizza").getOrThrow()

        // 1. Empty
        val vmEmpty = FoodViewModel(repository, host.id, dispatcher)
        advanceUntilIdle()
        assertEquals("Noch keine Stimmen", vmEmpty.uiState.resultText)

        // 2. Single leader
        repository.castFoodVote(host.id, cat1.id).getOrThrow()
        val vmLeader = FoodViewModel(repository, host.id, dispatcher)
        advanceUntilIdle()
        assertEquals("Aktuell vorne: Burger", vmLeader.uiState.resultText)

        // 3. Tie
        repository.castFoodVote(guest.id, cat2.id).getOrThrow()
        val vmTie = FoodViewModel(repository, host.id, dispatcher)
        advanceUntilIdle()
        assertEquals("Gleichstand: Burger und Pizza", vmTie.uiState.resultText)
    }

    @Test
    fun `remindMissingPlayers handles complete and incomplete voting states`() = runTest(dispatcher) {
        val cat = repository.addFoodCategory("Pasta").getOrThrow()

        // Guest has not voted yet
        repository.castFoodVote(host.id, cat.id).getOrThrow()
        val vm = FoodViewModel(repository, host.id, dispatcher)
        advanceUntilIdle()

        vm.remindMissingPlayers()
        assertTrue(vm.uiState.message?.contains("Erika Gast") == true)

        // Guest votes
        repository.castFoodVote(guest.id, cat.id).getOrThrow()
        vm.load()
        advanceUntilIdle()

        vm.remindMissingPlayers()
        assertEquals("Alle haben bereits abgestimmt.", vm.uiState.message)
    }

    @Test
    fun `restaurant editor only permits host to edit`() = runTest(dispatcher) {
        repository.saveRestaurant(host.id, "Altes Restaurant", "https://old.com").getOrThrow()

        // Non-host (guest)
        val vmGuest = FoodViewModel(repository, guest.id, dispatcher)
        advanceUntilIdle()
        vmGuest.beginRestaurantEditor()
        assertEquals("Nur der Gastgeber kann das Restaurant bearbeiten.", vmGuest.uiState.errorMessage)
        assertNull(vmGuest.uiState.restaurantEditor)

        // Host
        val vmHost = FoodViewModel(repository, host.id, dispatcher)
        advanceUntilIdle()
        vmHost.beginRestaurantEditor()
        assertNotNull(vmHost.uiState.restaurantEditor)
        assertEquals("Altes Restaurant", vmHost.uiState.restaurantEditor?.name)

        vmHost.updateRestaurantName("Neues Restaurant")
        vmHost.updateMenuUrl("https://new.com")
        vmHost.saveRestaurant()
        advanceUntilIdle()

        assertNull(vmHost.uiState.restaurantEditor)
        assertEquals("Restaurant gespeichert.", vmHost.uiState.message)
        assertEquals("Neues Restaurant", vmHost.uiState.restaurantName)
        assertEquals("https://new.com", vmHost.uiState.menuUrl)
    }

    @Test
    fun `dismissRestaurantEditor closes editor without saving`() = runTest(dispatcher) {
        val vmHost = FoodViewModel(repository, host.id, dispatcher)
        advanceUntilIdle()

        vmHost.beginRestaurantEditor()
        assertNotNull(vmHost.uiState.restaurantEditor)
        vmHost.dismissRestaurantEditor()
        assertNull(vmHost.uiState.restaurantEditor)
    }

    @Test
    fun `order editor validates price format and saves cents correctly`() = runTest(dispatcher) {
        val vm = FoodViewModel(repository, guest.id, dispatcher)
        advanceUntilIdle()

        vm.beginOrderEditor()
        assertNotNull(vm.uiState.orderEditor)

        vm.updateOrderDish("Pizza Funghi")
        vm.updateOrderNote("Ohne Zwiebeln")

        // Invalid letters
        vm.updateOrderPrice("abc")
        vm.saveOrder()
        assertEquals("Bitte einen gültigen Preis mit höchstens zwei Nachkommastellen eingeben.", vm.uiState.editorError)

        // Invalid 3 decimal digits
        vm.updateOrderPrice("10,999")
        vm.saveOrder()
        assertEquals("Bitte einen gültigen Preis mit höchstens zwei Nachkommastellen eingeben.", vm.uiState.editorError)

        // Valid comma price
        vm.updateOrderPrice("11,50")
        vm.saveOrder()
        advanceUntilIdle()

        assertNull(vm.uiState.orderEditor)
        assertEquals("Bestellung gespeichert.", vm.uiState.message)
        assertEquals(1, vm.uiState.orders.size)
        assertEquals("11,50 €", vm.uiState.orders[0].price)
        assertEquals("Pizza Funghi", vm.uiState.orders[0].dish)
        assertEquals("Ohne Zwiebeln", vm.uiState.orders[0].note)

        // Open editor again: should prepopulate existing order
        vm.beginOrderEditor()
        assertEquals("Pizza Funghi", vm.uiState.orderEditor?.dish)
        assertEquals("11,50", vm.uiState.orderEditor?.price)
        vm.dismissOrderEditor()
        assertNull(vm.uiState.orderEditor)
    }

    @Test
    fun `deleteOrder removes order and updates total price`() = runTest(dispatcher) {
        repository.saveFoodOrder(guest.id, "Salat", "", 750L).getOrThrow()
        val vm = FoodViewModel(repository, guest.id, dispatcher)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.orders.size)
        val orderId = vm.uiState.orders[0].id

        vm.deleteOrder(orderId)
        advanceUntilIdle()

        assertEquals("Bestellung gelöscht.", vm.uiState.message)
        assertEquals(0, vm.uiState.orders.size)
        assertEquals("0,00 €", vm.uiState.totalPrice)
    }

    @Test
    fun `castVote adds vote and updates category selection`() = runTest(dispatcher) {
        val cat = repository.addFoodCategory("Asiatisch").getOrThrow()
        val vm = FoodViewModel(repository, guest.id, dispatcher)
        advanceUntilIdle()

        assertFalse(vm.uiState.categories[0].isSelected)

        vm.castVote(cat.id)
        advanceUntilIdle()

        assertEquals("Essensstimme gespeichert.", vm.uiState.message)
        assertTrue(vm.uiState.categories[0].isSelected)
        assertEquals(1, vm.uiState.categories[0].voteCount)
    }

    @Test
    fun `category management allows adding and deleting categories`() = runTest(dispatcher) {
        val vm = FoodViewModel(repository, host.id, dispatcher)
        advanceUntilIdle()

        vm.beginAddCategory()
        assertNotNull(vm.uiState.categoryEditor)

        vm.updateCategoryName("Döner")
        vm.saveCategory()
        advanceUntilIdle()

        assertEquals("Döner wurde hinzugefügt.", vm.uiState.message)
        assertEquals(1, vm.uiState.categories.size)

        val catId = vm.uiState.categories[0].id
        vm.deleteCategory(catId)
        advanceUntilIdle()

        assertEquals("Kategorie wurde gelöscht.", vm.uiState.message)
        assertEquals(0, vm.uiState.categories.size)
    }

    @Test
    fun `dismissCategoryEditor and clearMessage work as expected`() = runTest(dispatcher) {
        val vm = FoodViewModel(repository, host.id, dispatcher)
        advanceUntilIdle()

        vm.beginAddCategory()
        vm.dismissCategoryEditor()
        assertNull(vm.uiState.categoryEditor)

        vm.remindMissingPlayers()
        assertNotNull(vm.uiState.message)
        vm.clearMessage()
        assertNull(vm.uiState.message)
        assertNull(vm.uiState.errorMessage)
    }

    @Test
    fun `FoodViewModel factory creates valid ViewModel instance`() {
        val factory = FoodViewModel.factory(repository, host.id)
        val vm = factory.create(FoodViewModel::class.java)
        assertNotNull(vm)
    }
}
