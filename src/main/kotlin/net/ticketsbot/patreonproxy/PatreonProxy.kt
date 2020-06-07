package net.ticketsbot.patreonproxy

import com.patreon.PatreonAPI
import com.patreon.PatreonOAuth
import net.ticketsbot.patreonproxy.config.Config
import net.ticketsbot.patreonproxy.config.JSON
import net.ticketsbot.patreonproxy.config.JSONConfiguration
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

        var poller = Poller(PatreonAPI(tokens.accessToken), config.config)

        val server = Server(config.config)
        server.run()

        while(true) {
            if (System.currentTimeMillis() > refreshAfter) {
                tokens = handleOauth(config)
                poller = Poller(PatreonAPI(tokens.accessToken), config.config)

                refreshAfter = System.currentTimeMillis() +
                        TimeUnit.SECONDS.toMillis(tokens.expiresIn.toLong()) -
                        TimeUnit.DAYS.toMillis(1)
            }

            try {
                poller.run()
                Thread.sleep(15 * 1000)
            } catch(ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun handleOauth(config: JSON): PatreonOAuth.TokensResponse {
        fun loadExisting(): PatreonOAuth.TokensResponse? {
            val expires = config.config.getGenericOrNull<Long>("oauth.tokens.expires") ?: return null
            if (expires - TimeUnit.DAYS.toMillis(1) > System.currentTimeMillis()) {
                return PatreonOAuth.TokensResponse(
                    config.config.getGenericOrNull<String>("oauth.tokens.access_token") ?: return null,
                    config.config.getGenericOrNull<String>("oauth.tokens.refresh_token") ?: return null,
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
            config.config.getGenericOrNull<String>("oauth.client_id"),
            config.config.getGenericOrNull<String>("oauth.client_secret"),
            config.config.getGenericOrNull<String>("oauth.redirect_uri")
        )

        val tokens = oauth.refreshTokens(config.config.getGenericOrNull<String>("oauth.tokens.refresh_token"))
        val expiry = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(tokens.expiresIn.toLong())

        config.config.set("oauth.tokens.refresh_token", tokens.refreshToken)
        config.config.set("oauth.tokens.access_token", tokens.accessToken)
        config.config.set("oauth.tokens.expires", expiry)
        config.save()

        return tokens
    }

    private fun loadConfig(): JSON {
        val cfg = Config()
        cfg.load()
        return cfg
    }
}