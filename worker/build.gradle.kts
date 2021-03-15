import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
    java
    application
}

application {
    mainClass.set("com.github.k0kubun.gitstar_ranking.GitstarRankingAppKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")
    implementation("com.google.guava:guava:30.1-jre")
    implementation("io.sentry:sentry:4.3.0")
    implementation("net.jodah:failsafe:2.4.0")
    implementation("org.jooq:jooq:3.14.8")
    implementation("org.postgresql:postgresql:42.2.19")

    val jerseyVersion = "2.33"
    implementation("org.glassfish.jersey.core:jersey-client:$jerseyVersion")
    implementation("org.glassfish.jersey.inject:jersey-hk2:$jerseyVersion")

    implementation(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.12.2"))
    implementation("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
