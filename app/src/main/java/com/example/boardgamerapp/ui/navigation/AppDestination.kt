package com.example.boardgamerapp.ui.navigation

import androidx.annotation.DrawableRes
import com.example.boardgamerapp.R

enum class AppDestination(
    val label: String,
    val title: String,
    val description: String,
    @param:DrawableRes val icon: Int,
) {
    GAME_NIGHT(
        label = "Termin",
        title = "Nächster Spieleabend",
        description = "Hier findest du bald Termin, Gastgeber und Ort.",
        icon = R.drawable.ic_home,
    ),
    GAMES(
        label = "Spiele",
        title = "Spiele",
        description = "Hier könnt ihr bald Spiele vorschlagen und abstimmen.",
        icon = R.drawable.ic_dice,
    ),
    FOOD(
        label = "Essen",
        title = "Essensabstimmung",
        description = "Stimmt über die Essensrichtung für den Abend ab.",
        icon = R.drawable.ic_restaurant,
    ),
    REVIEW(
        label = "Bewertung",
        title = "Abschluss und Bewertung",
        description = "Schließt Spieleabende ab und bewertet sie.",
        icon = R.drawable.ic_favorite,
    ),
    GROUPS(
        label = "Gruppen",
        title = "Gruppen",
        description = "Erstelle oder tritt einer Spielgruppe bei.",
        icon = R.drawable.ic_account_box,
    ),
    PROFILE(
        label = "Profil",
        title = "Profil",
        description = "Hier findest du deine Kontoinformationen und Logout.",
        icon = R.drawable.ic_account_box,
    ),
}
