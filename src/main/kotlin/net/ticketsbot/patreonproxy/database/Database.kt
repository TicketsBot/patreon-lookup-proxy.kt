package net.ticketsbot.patreonproxy.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp

object Database {

    private lateinit var conn: Connection

    fun connect() {
        conn = DriverManager.getConnection(
            System.getenv("DATABASE_URI"),
            System.getenv("DATABASE_USER"),
            System.getenv("DATABASE_PASSWORD")
        )
    }

    fun createSchema() {
        val query = """
            CREATE TABLE IF NOT EXISTS patreon_keys(
                "client_id" VARCHAR(255) NOT NULL,
                "access_token" VARCHAR(255) NOT NULL,
                "refresh_token" VARCHAR(255) NOT NULL,
                "expires" TIMESTAMPTZ NOT NULL,
                PRIMARY KEY("client_id")
            );
        """.trimIndent()

        val statement = conn.createStatement()
        statement.execute(query)
    }

    fun getTokens(clientId: String = getClientId()): Tokens? {
        val query = """
            SELECT
                "access_token", "refresh_token", "expires"
            FROM
                patreon_keys
            WHERE
                "client_id" = ?
            ;
        """.trimIndent()

        val statement = conn.prepareStatement(query)
        statement.queryTimeout = 10

        statement.setString(1, clientId)

        val rs = statement.executeQuery()
        return if (rs.next()) {
            Tokens(
                rs.getString(1),
                rs.getString(2),
                rs.getTimestamp(3)
            )
        } else {
            null
        }
    }

    fun updateTokens(clientId: String = getClientId(), tokens: Tokens) {
        val query = """
            INSERT INTO
                patreon_keys
            VALUES
                (?, ?, ?, ?)
            ON CONFLICT ("client_id") DO
            UPDATE
                SET "access_token" = EXCLUDED.access_token,
                    "refresh_token" = EXCLUDED.refresh_token,
                    "expires" = EXCLUDED.expires
        """.trimIndent()

        val statement = conn.prepareStatement(query)
        statement.setString(1, clientId)
        statement.setString(2, tokens.accessToken)
        statement.setString(3, tokens.refreshToken)
        statement.setTimestamp(4, tokens.expires)

        statement.executeUpdate()
    }

    fun getClientId() = System.getenv("PATREON_CLIENT_ID")

    data class Tokens(
        val accessToken: String,
        val refreshToken: String,
        val expires: Timestamp
    )
}