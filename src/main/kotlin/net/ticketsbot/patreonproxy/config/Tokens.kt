package net.ticketsbot.patreonproxy.config

@FileName("/data/tokens.json")
class Tokens : JSON() {
    val accessToken = this.config.getGenericOrNull<String>("access_token")
    val refreshToken = this.config.getGenericOrNull<String>("refresh_token")
    val expires = this.config.getGenericOrNull<Long>("access_token")
}