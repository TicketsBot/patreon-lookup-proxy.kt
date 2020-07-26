package net.ticketsbot.patreonproxy

import com.patreon.PatreonAPI
import com.patreon.PatreonOAuth
import net.ticketsbot.patreonproxy.database.Database
import net.ticketsbot.patreonproxy.http.Server
import net.ticketsbot.patreonproxy.patreon.Poller
import java.lang.Exception
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.system.exitProcess

class PatreonProxy : Runnable {

    override fun run() {
        Database.connect()
        Database.createSchema()

        var currentTokens = Database.getTokens()
        if (currentTokens == null) {
            println("getTokens returned null")
            exitProcess(-1)
        }

        var tokens = handleOauth(currentTokens)
        var refreshAfter = System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(tokens.expiresIn.toLong()) -
                TimeUnit.DAYS.toMillis(1)

        var poller = Poller(PatreonAPI(tokens.accessToken))

        val server = Server()
        var serverStarted = false // We want to do an initial poll before starting the server to prevent serving results before we have received data

        while(true) {
            if (System.currentTimeMillis() > refreshAfter) {
                try {
                    currentTokens = Database.getTokens()
                    if(currentTokens == null) {
                        println("getTokens returned null, continuing")
                        Thread.sleep(15 * 1000)
                        continue
                    }

                    tokens = handleOauth(currentTokens)
                    poller = Poller(PatreonAPI(tokens.accessToken))

                    refreshAfter = System.currentTimeMillis() +
                            TimeUnit.SECONDS.toMillis(tokens.expiresIn.toLong()) -
                            TimeUnit.DAYS.toMillis(1)
                } catch(ex: Exception) {
                    Thread.sleep(15 * 1000)
                    continue
                }
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

    private fun handleOauth(tokens: Database.Tokens): PatreonOAuth.TokensResponse {
        fun loadExisting(): PatreonOAuth.TokensResponse? {
            if (tokens.expires.toInstant().minusMillis(TimeUnit.DAYS.toMillis(1)).toEpochMilli() > System.currentTimeMillis()) {
                return PatreonOAuth.TokensResponse(
                    tokens.accessToken,
                    tokens.refreshToken,
                    TimeUnit.MILLISECONDS.toSeconds(tokens.expires.toInstant().minusMillis(System.currentTimeMillis()).toEpochMilli()).toInt(),
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

        Database.updateTokens(tokens = Database.Tokens(
            newTokens.accessToken, newTokens.refreshToken, Timestamp(expiry)
        ))

        return newTokens
    }
}