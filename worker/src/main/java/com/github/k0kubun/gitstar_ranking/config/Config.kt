package com.github.k0kubun.gitstar_ranking.config

class Config(env: Map<String, String>) {
    val databaseConfig: DatabaseConfig = DatabaseConfig(env)
    val queueConfig: QueueConfig

    init {
        queueConfig = QueueConfig()
    }
}
