package net.ticketsbot.patreonproxy.config

// can't get dynamically from an envvar, sorry
@FileName("/data/tokens.json")
class Tokens : JSON() {
    val accessToken: String?

        get() = this.config.getGenericOrNull<String>("access_token")
    val refreshToken: String?
        get() =  this.config.getGenericOrNull<String>("refresh_token")

    val expires: Long?
        get() =  this.config.getGenericOrNull<Long>("access_token")
}