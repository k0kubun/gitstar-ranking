plugins {
    id("java")
    id("application")
}

application {
    mainClass.set("com.github.k0kubun.gitstar_ranking.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha4")
    implementation("com.google.guava:guava:21.0")
    implementation("com.google.http-client:google-http-client:1.22.0")
    implementation("io.sentry:sentry:1.5.2")
    implementation("javax.json:javax.json-api:1.1")
    implementation("javax.ws.rs:javax.ws.rs-api:2.0.1")
    implementation("org.postgresql:postgresql:42.2.19")
    implementation("org.antlr:stringtemplate:3.2.1") // Using ST3 because ST4 isn"t supported by JDBI 2
    implementation("org.glassfish:javax.json:1.1")
    implementation("org.jboss.resteasy:resteasy-jackson2-provider:3.1.2.Final")
    implementation("org.jdbi:jdbi:2.78")
    testImplementation("junit:junit:4.12")
}
