package com.example.boardgamerapp

import com.example.boardgamerapp.ui.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {
    @Test
    fun `navigation contains the three iteration zero destinations`() {
        assertEquals(
            listOf("Termin", "Spiele", "Profil"),
            AppDestination.entries.map { it.label },
        )
    }

    @Test
    fun `every destination provides visible placeholder content`() {
        assertTrue(AppDestination.entries.all { it.title.isNotBlank() })
        assertTrue(AppDestination.entries.all { it.description.isNotBlank() })
    }
}
