package com.example.boardgamerapp.domain.model

fun nextHost(
    players: List<Player>,
    lastHostId: String?,
): Player? {
    val orderedPlayers = players.sortedBy { it.hostOrder }
    if (orderedPlayers.isEmpty()) return null

    val lastHostIndex = orderedPlayers.indexOfFirst { it.id == lastHostId }
    val nextIndex = if (lastHostIndex < 0) 0 else (lastHostIndex + 1) % orderedPlayers.size
    return orderedPlayers[nextIndex]
}
