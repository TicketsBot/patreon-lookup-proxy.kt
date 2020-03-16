package net.ticketsbot.patreonproxy

import com.patreon.PatreonAPI
import net.ticketsbot.patreonproxy.config.Config
import net.ticketsbot.patreonproxy.config.config
import net.ticketsbot.patreonproxy.http.Server
import net.ticketsbot.patreonproxy.patreon.Poller
import java.lang.Exception

class PatreonProxy : Runnable {

    override fun run() {
        loadConfig()
    }

    private fun loadConfig() {
        val cfg = Config()
        cfg.load()

        config = cfg.config

        val poller = Poller(PatreonAPI(config.getGenericOrNull<String>("oauth.accesstoken")))

        val server = Server()
        server.run()

        while(true) {
            try {
                poller.run()
                Thread.sleep(15 * 1000)
            } catch(ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}