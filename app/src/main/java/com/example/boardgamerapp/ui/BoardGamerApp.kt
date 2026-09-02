package com.example.boardgamerapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val networkMonitor = remember { com.example.boardgamerapp.data.network.LiveNetworkMonitor(context.applicationContext) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
    val notificationHelper = remember { com.example.boardgamerapp.data.notification.AppNotificationHelper(context.applicationContext) }
    val firebaseGameNightRepository = remember { FirebaseGameNightRepository() }
    val currentPlayerId = userUid.hashCode().toLong()
    val dashboardViewModel: DashboardViewModel = viewModel(
        key = "dashboard-$userUid",
        factory = DashboardViewModel.factory(
            repository = firebaseGameNightRepository,
            currentPlayerId = currentPlayerId,
            onSendNotification = notificationHelper::sendNotification,
        ),
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

    var selectedGroupId by rememberSaveable(userUid) { mutableStateOf<String?>(null) }
    var selectedGameNightDocId by rememberSaveable(userUid) { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(firebaseGameNightRepository) {
        val groupId = selectedGroupId
        val gameNightDocId = selectedGameNightDocId
        if (groupId != null && gameNightDocId != null) {
            firebaseGameNightRepository.selectGameNight(groupId, gameNightDocId)
        }
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
        Scaffold(
            topBar = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isOnline,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
                ) {
                    androidx.compose.material3.Surface(
                        color = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "⚠️ Keine Internetverbindung – Aktionen können fehlschlagen.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            when (currentDestination) {
                AppDestination.GAME_NIGHT -> DashboardScreen(
                    uiState = dashboardViewModel.uiState,
                    onRetry = dashboardViewModel::loadGameNight,
                    onEditGameNight = dashboardViewModel::beginEditGameNight,
                    onGameNightDateChange = dashboardViewModel::updateGameNightEditorDate,
                    onGameNightTimeChange = dashboardViewModel::updateGameNightEditorTime,
                    onGameNightHostChange = dashboardViewModel::updateGameNightEditorHost,
                    onSaveEditedGameNight = dashboardViewModel::saveEditedGameNight,
                    onDismissGameNightEditor = dashboardViewModel::dismissGameNightEditor,
                    onConfirmAttending = dashboardViewModel::confirmAttending,
                    onBeginStatusReport = dashboardViewModel::beginStatusReport,
                    onSelectStatusReportType = dashboardViewModel::selectStatusReportType,
                    onDeclineReasonChange = dashboardViewModel::updateDeclineReason,
                    onSelectLateNoticePreset = dashboardViewModel::selectLateNoticePreset,
                    onLateNoticeCustomMinutesChange = dashboardViewModel::updateLateNoticeCustomMinutes,
                    onSaveStatusReport = dashboardViewModel::saveStatusReport,
                    onDismissStatusReport = dashboardViewModel::dismissStatusReport,
                    onSelectGameNight = { groupId, gameNightDocId ->
                        selectedGroupId = groupId
                        selectedGameNightDocId = gameNightDocId
                        dashboardViewModel.selectGameNight(groupId, gameNightDocId)
                        gamesViewModel.loadGames()
                        foodViewModel.load()
                        reviewViewModel.load()
                    },
                    onPlanGameNight = dashboardViewModel::planNextGameNight,
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
