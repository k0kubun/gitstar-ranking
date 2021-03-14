package com.github.k0kubun.gitstar_ranking

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource

data class DatabaseConfiguration(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val database: String,
) {
    val dataSource: DataSource = PGSimpleDataSource().also {
        it.setUrl("jdbc:postgresql://$host:$port/$database")
        it.user = user
        it.password = password
    }

    constructor(env: Map<String, String> = System.getenv()) : this(
        host = env.getOrDefault("DATABASE_HOST", "127.0.0.1"),
        port = env.getOrDefault("DATABASE_PORT", "5432").toInt(),
        user = env.getOrDefault("DATABASE_USER", "postgres"),
        password = env.getOrDefault("DATABASE_PASSWORD", ""),
        database = env.getOrDefault("DATABASE_NAME", "gitstar_ranking"),
    )
}

data class QueueConfiguration(
    val userRankingQueue: BlockingQueue<Boolean> = LinkedBlockingQueue(),
    val userStarScanQueue: BlockingQueue<Boolean> = LinkedBlockingQueue(),
    val userFullScanQueue: BlockingQueue<Boolean> = LinkedBlockingQueue(),
)

data class GitstarRankingConfiguration(
    val database: DatabaseConfiguration = DatabaseConfiguration(),
    val queue: QueueConfiguration = QueueConfiguration(),
)
