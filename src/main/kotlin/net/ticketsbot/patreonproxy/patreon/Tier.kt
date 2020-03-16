package net.ticketsbot.patreonproxy.patreon

enum class Tier(val patreonId: Int, val tierId: Int) {
    PREMIUM(4071609, 0),
    ;

    companion object {
        fun getTierByPatreonId(id: Int?) = values().firstOrNull { it.patreonId == id }
    }
}