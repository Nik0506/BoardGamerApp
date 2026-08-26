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
        icon = R.drawable.ic_favorite,
    ),
    PROFILE(
        label = "Profil",
        title = "Profil",
        description = "Hier verwaltest du bald dein Profil und die Spielgruppe.",
        icon = R.drawable.ic_account_box,
    ),
}
