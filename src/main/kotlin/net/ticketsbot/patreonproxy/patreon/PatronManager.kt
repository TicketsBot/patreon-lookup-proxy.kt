package net.ticketsbot.patreonproxy.patreon

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

object PatronManager {
    var patrons = ConcurrentHashMap<String, Tier>()
    val lock = ReentrantReadWriteLock()
}