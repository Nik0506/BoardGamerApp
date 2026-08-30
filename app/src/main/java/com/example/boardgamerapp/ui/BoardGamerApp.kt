package com.example.boardgamerapp.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.boardgamerapp.data.repository.FirebaseGameNightRepository
import com.example.boardgamerapp.ui.auth.AuthScreen
import com.example.boardgamerapp.ui.dashboard.DashboardScreen
import com.example.boardgamerapp.ui.group.GroupManagementScreen
import com.example.boardgamerapp.ui.dashboard.DashboardViewModel
import com.example.boardgamerapp.ui.games.GamesScreen
import com.example.boardgamerapp.ui.games.GamesViewModel
import com.example.boardgamerapp.ui.food.FoodScreen
import com.example.boardgamerapp.ui.food.FoodViewModel
import com.example.boardgamerapp.ui.navigation.AppDestination
import com.example.boardgamerapp.ui.profile.ProfileScreen
import com.example.boardgamerapp.ui.review.ReviewScreen
import com.example.boardgamerapp.ui.review.ReviewViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun BoardGamerApp() {
    val auth = FirebaseAuth.getInstance()
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { currentUser = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val signedInUser = currentUser
    if (signedInUser == null) {
        AuthScreen(onSignedIn = { currentUser = auth.currentUser })
        return
    }

    key(signedInUser.uid) {
        SignedInApp(signedInUser.uid)
    }
}

@Composable
private fun SignedInApp(userUid: String) {
    val firebaseGameNightRepository = remember { FirebaseGameNightRepository() }
    val currentPlayerId = userUid.hashCode().toLong()
    val dashboardViewModel: DashboardViewModel = viewModel(
        key = "dashboard-$userUid",
        factory = DashboardViewModel.factory(firebaseGameNightRepository, currentPlayerId),
    )
    val gamesViewModel: GamesViewModel = viewModel(
        key = "games-$userUid",
        factory = GamesViewModel.factory(
            firebaseGameNightRepository,
            firebaseGameNightRepository,
            firebaseGameNightRepository,
            currentPlayerId,
        ),
    )
    val reviewViewModel: ReviewViewModel = viewModel(
        key = "review-$userUid",
        factory = ReviewViewModel.factory(firebaseGameNightRepository, currentPlayerId),
    )
    val foodViewModel: FoodViewModel = viewModel(
        key = "food-$userUid",
        factory = FoodViewModel.factory(firebaseGameNightRepository, currentPlayerId),
    )
    var currentDestination by rememberSaveable(userUid) {
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
                            AppDestination.GROUPS -> Unit
                            AppDestination.PROFILE -> Unit
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
                    onAddLateNotice = dashboardViewModel::beginLateNotice,
                    onSelectLateNoticePreset = dashboardViewModel::selectLateNoticePreset,
                    onLateNoticeCustomMinutesChange = dashboardViewModel::updateLateNoticeCustomMinutes,
                    onSaveLateNotice = dashboardViewModel::saveLateNotice,
                    onDismissLateNoticeEditor = dashboardViewModel::dismissLateNoticeEditor,
                    onDismissMessage = dashboardViewModel::clearMessage,
                    modifier = Modifier.padding(innerPadding),
                )

                AppDestination.GROUPS -> GroupManagementScreen(
                    onGroupReady = { currentDestination = AppDestination.GAME_NIGHT },
                    userUid = userUid,
                    modifier = Modifier.padding(innerPadding),
                )

                AppDestination.PROFILE -> ProfileScreen(
                    onSignOut = {},
                    modifier = Modifier.padding(innerPadding),
                )

                AppDestination.GAMES -> GamesScreen(
                    uiState = gamesViewModel.uiState,
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
