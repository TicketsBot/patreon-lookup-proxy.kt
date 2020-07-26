package net.ticketsbot.patreonproxy.http

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import net.ticketsbot.patreonproxy.patreon.PatronManager
import kotlin.concurrent.read

class Server : Runnable {

    override fun run() {
        val host = System.getenv("SERVER_HOST") ?: "0.0.0.0"
        val port = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080

        val server = embeddedServer(Netty, port, host, module = getModule())
        server.start(false)
    }
}

private fun getModule(): Application.() -> Unit {
    return fun Application.() {
        install(DefaultHeaders)
        install(ContentNegotiation) {
            jackson()
        }

        install(Routing) {
            get("/ping") {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            }

            get("/ispremium") {
                val key = call.request.queryParameters["key"]
                val serverKey = System.getenv("SERVER_KEY")

                if(serverKey == null || serverKey != key) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Invalid secret key"))
                    return@get
                }

                val id = call.request.queryParameters["id"]

                val tier = PatronManager.lock.read {
                    PatronManager.patrons[id]
                }

                val isPremium = tier != null

                val res = mutableMapOf<String, Any>("premium" to isPremium)

                if(tier != null) {
                    res["tier"] = tier.tierId
                }

                call.respond(HttpStatusCode.OK, res)
            }
        }
    }
}

