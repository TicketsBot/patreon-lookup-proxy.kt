package net.ticketsbot.patreonproxy

import com.patreon.PatreonAPI
import com.patreon.PatreonOAuth
import net.ticketsbot.patreonproxy.config.Tokens
import net.ticketsbot.patreonproxy.http.Server
import net.ticketsbot.patreonproxy.patreon.Poller
import java.lang.Exception
import java.util.concurrent.TimeUnit

class PatreonProxy : Runnable {

    override fun run() {
        val config = loadConfig()

        var tokens = handleOauth(config)
        var refreshAfter = System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(tokens.expiresIn.toLong()) -
                TimeUnit.DAYS.toMillis(1)

        var poller = Poller(PatreonAPI(tokens.accessToken))

        val server = Server(config.config)
        var serverStarted = false // We want to do an initial poll before starting the server to prevent serving results before we have received data

        while(true) {
            if (System.currentTimeMillis() > refreshAfter) {
                tokens = handleOauth(config)
                poller = Poller(PatreonAPI(tokens.accessToken))

                refreshAfter = System.currentTimeMillis() +
                        TimeUnit.SECONDS.toMillis(tokens.expiresIn.toLong()) -
                        TimeUnit.DAYS.toMillis(1)
            }

            try {
                poller.run()

                if (!serverStarted) {
                    serverStarted = true
                    server.run()
                }

                Thread.sleep(15 * 1000)
            } catch(ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun handleOauth(tokens: Tokens): PatreonOAuth.TokensResponse {
        fun loadExisting(): PatreonOAuth.TokensResponse? {
            val expires = tokens.expires ?: return null
            if (expires - TimeUnit.DAYS.toMillis(1) > System.currentTimeMillis()) {
                return PatreonOAuth.TokensResponse(
                    tokens.accessToken ?: return null,
                    tokens.refreshToken ?: return null,
                    TimeUnit.MILLISECONDS.toSeconds(expires - System.currentTimeMillis()).toInt(),
                    "",
                    ""
                )
            }

            return null
        }

        loadExisting()?.let {
            return it
        }

        val oauth = PatreonOAuth(
            System.getenv("PATREON_CLIENT_ID"),
            System.getenv("PATREON_CLIENT_SECRET"),
            System.getenv("PATREON_REDIRECT_URI")
        )

        val newTokens = oauth.refreshTokens(tokens.refreshToken)
        val expiry = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(newTokens.expiresIn.toLong())

        tokens.config.set("refresh_token", newTokens.refreshToken)
        tokens.config.set("access_token", newTokens.accessToken)
        tokens.config.set("expires", expiry)
        tokens.save()

        return newTokens
    }

    private fun loadConfig(): Tokens {
        val cfg = Tokens()
        cfg.load()
        return cfg
    }
}