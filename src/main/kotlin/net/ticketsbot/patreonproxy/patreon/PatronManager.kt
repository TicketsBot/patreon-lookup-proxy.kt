package net.ticketsbot.patreonproxy.patreon

import java.util.concurrent.ConcurrentHashMap

object PatronManager {
    var patrons = ConcurrentHashMap<String, Tier>()
}