package com.example.boardgamerapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.ui.dashboard.DashboardAttendanceUiModel
import com.example.boardgamerapp.ui.dashboard.DashboardPlayerUiModel
import com.example.boardgamerapp.ui.dashboard.DashboardScreen
import com.example.boardgamerapp.ui.dashboard.DashboardUiState
import com.example.boardgamerapp.ui.dashboard.GameNightEditorUiState
import com.example.boardgamerapp.ui.dashboard.GameNightPickerUiModel
import com.example.boardgamerapp.ui.dashboard.GameNightUiModel
import com.example.boardgamerapp.ui.dashboard.HostDeclineOption
import com.example.boardgamerapp.ui.dashboard.StatusReportEditorUiState
import com.example.boardgamerapp.ui.dashboard.StatusReportType
import com.example.boardgamerapp.ui.food.FoodCategoryUiModel
import com.example.boardgamerapp.ui.food.FoodOrderUiModel
import com.example.boardgamerapp.ui.food.FoodPlayerUiModel
import com.example.boardgamerapp.ui.food.FoodScreen
import com.example.boardgamerapp.ui.food.FoodUiState
import com.example.boardgamerapp.ui.food.OrderEditor
import com.example.boardgamerapp.ui.food.RestaurantEditor
import com.example.boardgamerapp.ui.games.GamePlayerUiModel
import com.example.boardgamerapp.ui.games.GameSuggestionEditorUiState
import com.example.boardgamerapp.ui.games.GameSuggestionUiModel
import com.example.boardgamerapp.ui.games.GamesScreen
import com.example.boardgamerapp.ui.games.GamesUiState
import com.example.boardgamerapp.ui.players.PlayerEditorUiState
import com.example.boardgamerapp.ui.players.PlayerUiModel
import com.example.boardgamerapp.ui.players.PlayersScreen
import com.example.boardgamerapp.ui.players.PlayersUiState
import com.example.boardgamerapp.ui.review.RatingEditorUiState
import com.example.boardgamerapp.ui.review.ReviewAveragesUiModel
import com.example.boardgamerapp.ui.review.ReviewPlayerUiModel
import com.example.boardgamerapp.ui.review.ReviewScreen
import com.example.boardgamerapp.ui.review.ReviewUiState
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

class ResponsiveDesignTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setConstrainedContent(width: Dp, height: Dp, content: @Composable () -> Unit) {
        composeRule.setContent {
            Box(modifier = Modifier.requiredSize(width, height)) {
                content()
            }
        }
    }

    // --- Phone Landscape Tests (780 x 360 dp - critical for vertical scrolling & keyboard) ---

    @Test
    fun dashboardScreen_rendersCorrectlyInLandscape() {
        setConstrainedContent(width = 780.dp, height = 360.dp) {
            DashboardScreen(
                uiState = sampleDashboardState(),
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Freitag, 25. September 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Mein Teilnahmestatus").assertIsDisplayed()
    }

    @Test
    fun dashboardEditDialog_rendersAndIsScrollableInLandscape() {
        setConstrainedContent(width = 780.dp, height = 360.dp) {
            DashboardScreen(
                uiState = sampleDashboardState().copy(
                    gameNightEditor = GameNightEditorUiState(
                        gameNightId = 1L,
                        selectedDate = LocalDate.of(2026, 9, 25),
                        selectedTime = LocalTime.of(19, 0),
                        selectedHostId = 1L,
                    ),
                ),
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Spieleabend editieren").assertIsDisplayed()
        composeRule.onNodeWithText("Speichern").assertIsDisplayed()
        composeRule.onNodeWithText("Abbrechen").assertIsDisplayed()
    }

    @Test
    fun dashboardHostDeclineDialog_rendersAndIsScrollableInLandscape() {
        setConstrainedContent(width = 780.dp, height = 360.dp) {
            DashboardScreen(
                uiState = sampleDashboardState().copy(
                    statusReportEditor = StatusReportEditorUiState(
                        type = StatusReportType.DECLINED,
                        hostDeclineOption = HostDeclineOption.RESCHEDULE,
                    ),
                ),
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Absage durch Gastgeber").assertIsDisplayed()
        composeRule.onNodeWithText("Spieleabend verschieben").assertIsDisplayed()
    }

    @Test
    fun foodScreen_rendersCorrectlyInLandscape() {
        setConstrainedContent(width = 780.dp, height = 360.dp) {
            FoodScreen(
                uiState = sampleFoodState(),
                onCastVote = {},
                onAddCategory = {},
                onCategoryNameChange = {},
                onSaveCategory = {},
                onDismissCategoryEditor = {},
                onDeleteCategory = {},
                onRemindMissingPlayers = {},
                onEditRestaurant = {},
                onRestaurantNameChange = {},
                onMenuUrlChange = {},
                onSaveRestaurant = {},
                onDismissRestaurantEditor = {},
                onEditOrder = {},
                onOrderDishChange = {},
                onOrderNoteChange = {},
                onOrderPriceChange = {},
                onSaveOrder = {},
                onDismissOrderEditor = {},
                onDeleteOrder = {},
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Essensabstimmung").assertIsDisplayed()
        composeRule.onNodeWithText("Aktuell vorne: Pizza").assertIsDisplayed()
    }

    @Test
    fun foodOrderEditor_rendersAndIsScrollableInLandscape() {
        setConstrainedContent(width = 780.dp, height = 360.dp) {
            FoodScreen(
                uiState = sampleFoodState().copy(
                    orderEditor = OrderEditor(dish = "Pizza Funghi", note = "Knusprig", price = "9,50"),
                ),
                onCastVote = {},
                onAddCategory = {},
                onCategoryNameChange = {},
                onSaveCategory = {},
                onDismissCategoryEditor = {},
                onDeleteCategory = {},
                onRemindMissingPlayers = {},
                onEditRestaurant = {},
                onRestaurantNameChange = {},
                onMenuUrlChange = {},
                onSaveRestaurant = {},
                onDismissRestaurantEditor = {},
                onEditOrder = {},
                onOrderDishChange = {},
                onOrderNoteChange = {},
                onOrderPriceChange = {},
                onSaveOrder = {},
                onDismissOrderEditor = {},
                onDeleteOrder = {},
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Meine Bestellung").assertIsDisplayed()
        composeRule.onNodeWithText("Speichern").assertIsDisplayed()
        composeRule.onNodeWithText("Abbrechen").assertIsDisplayed()
    }

    @Test
    fun gamesScreen_rendersAndAllowsDialogInLandscape() {
        setConstrainedContent(width = 780.dp, height = 360.dp) {
            GamesScreen(
                uiState = sampleGamesState().copy(
                    editor = GameSuggestionEditorUiState(name = "Catan", description = "Strategie"),
                ),
                onAddSuggestion = {},
                onDeleteSuggestion = {},
                onCastVote = {},
                onNameChange = {},
                onDescriptionChange = {},
                onSaveSuggestion = {},
                onDismissEditor = {},
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Spielvorschläge").assertIsDisplayed()
        composeRule.onNodeWithText("Hinzufügen").assertIsDisplayed()
        composeRule.onNodeWithText("Abbrechen").assertIsDisplayed()
    }

    @Test
    fun reviewScreen_rendersAndAllowsDialogInLandscape() {
        setConstrainedContent(width = 780.dp, height = 360.dp) {
            ReviewScreen(
                uiState = sampleReviewState().copy(
                    editor = RatingEditorUiState(hostRating = 5, foodRating = 4, eveningRating = 5),
                ),
                onRetry = {},
                onFinishGameNight = {},
                onBeginReview = {},
                onHostRating = {},
                onFoodRating = {},
                onEveningRating = {},
                onCommentChange = {},
                onSaveReview = {},
                onDismissEditor = {},
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Spieleabend bewerten").assertIsDisplayed()
        composeRule.onNodeWithText("Speichern").assertIsDisplayed()
    }

    @Test
    fun playersScreen_rendersAndAllowsEditorInLandscape() {
        setConstrainedContent(width = 780.dp, height = 360.dp) {
            PlayersScreen(
                uiState = samplePlayersState().copy(
                    editor = PlayerEditorUiState(playerId = 1L, name = "Max", address = "Musterstraße 12"),
                ),
                onAddPlayer = {},
                onEditPlayer = {},
                onMovePlayer = { _, _ -> },
                onCreateNextGameNight = {},
                onNameChange = {},
                onAddressChange = {},
                onSavePlayer = {},
                onDismissEditor = {},
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Spieler bearbeiten").assertIsDisplayed()
        composeRule.onNodeWithText("Speichern").assertIsDisplayed()
    }

    // --- Tablet / Large Screen Tests (1280 x 800 dp) ---

    @Test
    fun dashboardAndFood_renderComfortablyOnTabletResolution() {
        setConstrainedContent(width = 1280.dp, height = 800.dp) {
            DashboardScreen(
                uiState = sampleDashboardState(),
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Freitag, 25. September 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Teilnahme der Gruppe").assertIsDisplayed()
    }

    // --- Sample State Helpers ---

    private fun sampleDashboardState() = DashboardUiState.Content(
        gameNight = GameNightUiModel(
            id = 1L,
            date = "Freitag, 25. September 2026",
            time = "19:00 Uhr",
            hostName = "Max Mustermann",
            hostId = 1L,
            location = "Musterstraße 12",
        ),
        players = listOf(
            DashboardPlayerUiModel(1L, "Max Mustermann"),
            DashboardPlayerUiModel(2L, "Erika Musterfrau"),
        ),
        selectedPlayerId = 1L,
        attendances = listOf(
            DashboardAttendanceUiModel(1L, "Max Mustermann", AttendanceStatusType.ATTENDING, isCurrentPlayer = true),
            DashboardAttendanceUiModel(2L, "Erika Musterfrau", AttendanceStatusType.PENDING),
        ),
    )

    private fun sampleFoodState() = FoodUiState(
        isLoading = false,
        gameNightDate = "Freitag, 25. September 2026",
        selectedPlayerId = 1L,
        hostId = 1L,
        restaurantName = "Pizzeria Da Luigi",
        categories = listOf(
            FoodCategoryUiModel(1L, "Pizza", 2, setOf(1L, 2L), isSelected = true),
        ),
        orders = listOf(
            FoodOrderUiModel(10L, 1L, "Max", "Pizza Salami", "", "8,50 €"),
        ),
        totalPrice = "8,50 €",
        resultText = "Aktuell vorne: Pizza",
    )

    private fun sampleGamesState() = GamesUiState(
        isLoading = false,
        gameNightDate = "Freitag, 25. September 2026",
        selectedPlayerId = 1L,
        suggestions = listOf(
            GameSuggestionUiModel(1L, "Catan", "Klassiker", 1L, "Max", "Freitag, 25. September 2026", setOf(1L), isSelected = true),
        ),
        resultText = "Aktuell vorne: Catan",
    )

    private fun sampleReviewState() = ReviewUiState.Content(
        gameNightId = 1L,
        date = "Freitag, 25. September 2026, 19:00 Uhr",
        hostName = "Max",
        isFinished = true,
        players = listOf(ReviewPlayerUiModel(1L, "Max", false)),
        selectedPlayerId = 1L,
        currentPlayerName = "Max",
        currentPlayerHasReviewed = false,
        reviewCount = 1,
        averages = ReviewAveragesUiModel("5,0", "4,5", "5,0"),
    )

    private fun samplePlayersState() = PlayersUiState(
        isLoading = false,
        players = listOf(
            PlayerUiModel(1L, "Max", "Musterstraße 12", 1),
            PlayerUiModel(2L, "Erika", "Neustraße 5", 2),
        ),
    )
}
