package com.example.boardgamerapp.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.boardgamerapp.data.repository.InMemoryGameNightRepository
import com.example.boardgamerapp.data.repository.RoomGameNightRepository
import com.example.boardgamerapp.ui.dashboard.DashboardScreen
import com.example.boardgamerapp.ui.dashboard.DashboardViewModel
import com.example.boardgamerapp.ui.games.GamesScreen
import com.example.boardgamerapp.ui.games.GamesViewModel
import com.example.boardgamerapp.ui.food.FoodScreen
import com.example.boardgamerapp.ui.food.FoodViewModel
import com.example.boardgamerapp.ui.navigation.AppDestination
import com.example.boardgamerapp.ui.players.PlayersScreen
import com.example.boardgamerapp.ui.players.PlayersViewModel
import com.example.boardgamerapp.ui.review.ReviewScreen
import com.example.boardgamerapp.ui.review.ReviewViewModel
import com.example.boardgamerapp.ui.theme.BoardGamerAppTheme

@Composable
fun BoardGamerApp() {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val repository = remember(context, isPreview) {
        if (isPreview) InMemoryGameNightRepository() else RoomGameNightRepository.create(context)
    }
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(repository),
    )
    val playersViewModel: PlayersViewModel = viewModel(
        factory = PlayersViewModel.factory(repository),
    )
    val gamesViewModel: GamesViewModel = viewModel(
        factory = GamesViewModel.factory(repository, repository, repository),
    )
    val reviewViewModel: ReviewViewModel = viewModel(
        factory = ReviewViewModel.factory(repository),
    )
    val foodViewModel: FoodViewModel = viewModel(
        factory = FoodViewModel.factory(repository),
    )
    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestination.GAME_NIGHT)
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painter = painterResource(destination.icon),
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = {
                        currentDestination = destination
                        when (destination) {
                            AppDestination.GAME_NIGHT -> dashboardViewModel.loadGameNight()
                            AppDestination.PROFILE -> playersViewModel.loadPlayers()
                            AppDestination.GAMES -> gamesViewModel.loadGames()
                            AppDestination.REVIEW -> reviewViewModel.load()
                            AppDestination.FOOD -> foodViewModel.load()
                        }
                    },
                )
            }
        },
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestination.GAME_NIGHT -> DashboardScreen(
                    uiState = dashboardViewModel.uiState,
                    onRetry = dashboardViewModel::loadGameNight,
                    onSelectPlayer = dashboardViewModel::selectPlayer,
                    onAddLateNotice = dashboardViewModel::beginLateNotice,
                    onSelectLateNoticePreset = dashboardViewModel::selectLateNoticePreset,
                    onLateNoticeCustomMinutesChange = dashboardViewModel::updateLateNoticeCustomMinutes,
                    onSaveLateNotice = dashboardViewModel::saveLateNotice,
                    onDismissLateNoticeEditor = dashboardViewModel::dismissLateNoticeEditor,
                    onDismissMessage = dashboardViewModel::clearMessage,
                    modifier = Modifier.padding(innerPadding),
                )

                AppDestination.PROFILE -> PlayersScreen(
                    uiState = playersViewModel.uiState,
                    onAddPlayer = playersViewModel::beginAddPlayer,
                    onEditPlayer = playersViewModel::beginEditPlayer,
                    onMovePlayer = playersViewModel::movePlayer,
                    onCreateNextGameNight = playersViewModel::createNextGameNight,
                    onNameChange = playersViewModel::updateEditorName,
                    onAddressChange = playersViewModel::updateEditorAddress,
                    onSavePlayer = playersViewModel::savePlayer,
                    onDismissEditor = playersViewModel::dismissEditor,
                    onDismissMessage = playersViewModel::clearMessage,
                    modifier = Modifier.padding(innerPadding),
                )

                AppDestination.GAMES -> GamesScreen(
                    uiState = gamesViewModel.uiState,
                    onSelectPlayer = gamesViewModel::selectPlayer,
                    onAddSuggestion = gamesViewModel::beginAddSuggestion,
                    onDeleteSuggestion = gamesViewModel::deleteSuggestion,
                    onCastVote = gamesViewModel::castVote,
                    onNameChange = gamesViewModel::updateEditorName,
                    onDescriptionChange = gamesViewModel::updateEditorDescription,
                    onSaveSuggestion = gamesViewModel::saveSuggestion,
                    onDismissEditor = gamesViewModel::dismissEditor,
                    onDismissMessage = gamesViewModel::clearMessage,
                    modifier = Modifier.padding(innerPadding),
                )

                AppDestination.REVIEW -> ReviewScreen(
                    uiState = reviewViewModel.uiState,
                    onRetry = reviewViewModel::load,
                    onFinishGameNight = reviewViewModel::finishGameNight,
                    onSelectPlayer = reviewViewModel::selectPlayer,
                    onBeginReview = reviewViewModel::beginReview,
                    onHostRating = reviewViewModel::setHostRating,
                    onFoodRating = reviewViewModel::setFoodRating,
                    onEveningRating = reviewViewModel::setEveningRating,
                    onCommentChange = reviewViewModel::updateComment,
                    onSaveReview = reviewViewModel::saveReview,
                    onDismissEditor = reviewViewModel::dismissEditor,
                    onDismissMessage = reviewViewModel::clearMessage,
                    modifier = Modifier.padding(innerPadding),
                )

                AppDestination.FOOD -> FoodScreen(
                    uiState = foodViewModel.uiState,
                    onSelectPlayer = foodViewModel::selectPlayer,
                    onCastVote = foodViewModel::castVote,
                    onAddCategory = foodViewModel::beginAddCategory,
                    onCategoryNameChange = foodViewModel::updateCategoryName,
                    onSaveCategory = foodViewModel::saveCategory,
                    onDismissCategoryEditor = foodViewModel::dismissCategoryEditor,
                    onDeleteCategory = foodViewModel::deleteCategory,
                    onRemindMissingPlayers = foodViewModel::remindMissingPlayers,
                    onEditRestaurant = foodViewModel::beginRestaurantEditor,
                    onRestaurantNameChange = foodViewModel::updateRestaurantName,
                    onMenuUrlChange = foodViewModel::updateMenuUrl,
                    onSaveRestaurant = foodViewModel::saveRestaurant,
                    onDismissRestaurantEditor = foodViewModel::dismissRestaurantEditor,
                    onEditOrder = foodViewModel::beginOrderEditor,
                    onOrderDishChange = foodViewModel::updateOrderDish,
                    onOrderNoteChange = foodViewModel::updateOrderNote,
                    onOrderPriceChange = foodViewModel::updateOrderPrice,
                    onSaveOrder = foodViewModel::saveOrder,
                    onDismissOrderEditor = foodViewModel::dismissOrderEditor,
                    onDeleteOrder = foodViewModel::deleteOrder,
                    onDismissMessage = foodViewModel::clearMessage,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun BoardGamerAppPreview() {
    BoardGamerAppTheme(dynamicColor = false) {
        BoardGamerApp()
    }
}
