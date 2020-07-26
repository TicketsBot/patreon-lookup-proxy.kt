package net.ticketsbot.patreonproxy.patreon

import com.patreon.PatreonAPI
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.write

class Poller(val api: PatreonAPI) : Runnable {

    override fun run() {
        val campaignId = System.getenv("PATREON_CAMPAIGN_ID") ?: return

        val pledges = api.fetchAllPledges(campaignId)

        val patrons = pledges
            .filterNot { pledge -> pledge.patron.socialConnections.discord?.user_id == null } // Make sure user has Discord account linked
            .filter { pledge -> pledge.declinedSince == null } // Make sure user is still pledged
            .filterNot { pledge -> pledge.reward?.id == null } // Make sure user has a tier
            .mapNotNull { pledge ->
                val discordId = pledge.patron.socialConnections.discord.user_id
                val tier = Tier.getTierByPatreonId(pledge.reward.id.toIntOrNull())
                    ?: return@mapNotNull null
                discordId to tier
            }
            .toMap()

        PatronManager.lock.write {
            PatronManager.patrons = ConcurrentHashMap(patrons)
        }
    }
}