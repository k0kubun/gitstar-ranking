package com.github.k0kubun.gitstar_ranking.config

import org.postgresql.ds.PGSimpleDataSource
import com.github.k0kubun.gitstar_ranking.config.DatabaseConfig
import javax.sql.DataSource

class DatabaseConfig(private val env: Map<String, String>) {
    val dataSource: DataSource
        get() {
            val dataSource = PGSimpleDataSource()
            dataSource.setUrl(url)
            dataSource.user = user
            dataSource.password = password
            return dataSource
        }
    val url: String
        get() = String.format("jdbc:postgresql://%s:%d/%s",
            host,
            port,
            databaseName)
    val user: String
        get() = env["DATABASE_USER"] ?: DEFAULT_USER
    val password: String
        get() = env["DATABASE_PASSWORD"] ?: DEFAULT_PASSWORD
    private val host: String
        private get() = env["DATABASE_HOST"] ?: DEFAULT_HOST
    private val port: Int
        private get() = getIntegerOrDefault("DATABASE_PORT", DEFAULT_PORT)
    private val databaseName: String
        private get() = env["DATABASE_NAME"] ?: DEFAULT_DATABASE

    private fun getIntegerOrDefault(key: String, defaultValue: Int): Int {
        return if (env.containsKey(key)) {
            val value = env[key]
            value!!.toInt()
        } else {
            defaultValue
        }
    }

    companion object {
        private const val DEFAULT_HOST = "127.0.0.1"
        private const val DEFAULT_PORT = 5432
        private const val DEFAULT_USER = "postgres"
        private const val DEFAULT_PASSWORD = ""
        private const val DEFAULT_DATABASE = "gitstar_ranking"
    }
}
