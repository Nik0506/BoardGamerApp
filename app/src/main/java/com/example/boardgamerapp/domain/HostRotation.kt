package com.example.boardgamerapp.domain

import com.example.boardgamerapp.domain.model.Player

object HostRotation {
    fun nextHost(players: List<Player>, lastHostId: Long?): Player? {
        val orderedPlayers = players.sortedBy { it.hostOrder }
        if (orderedPlayers.isEmpty()) return null

        val lastHostIndex = orderedPlayers.indexOfFirst { it.id == lastHostId }
        return if (lastHostIndex == -1) {
            orderedPlayers.first()
        } else {
            orderedPlayers[(lastHostIndex + 1) % orderedPlayers.size]
        }
    }
}
