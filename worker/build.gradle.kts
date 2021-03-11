plugins {
    id("java")
    id("application")
}

application {
    mainClassName = "com.github.k0kubun.gitstar_ranking.Main"
}

repositories {
    mavenCentral()
}

dependencies {
    compile("ch.qos.logback:logback-classic:1.3.0-alpha4")
    compile("com.google.guava:guava:21.0")
    compile("com.google.http-client:google-http-client:1.22.0")
    compile("io.sentry:sentry:1.5.2")
    compile("javax.json:javax.json-api:1.1")
    compile("javax.ws.rs:javax.ws.rs-api:2.0.1")
    compile("org.postgresql:postgresql:42.2.19")
    compile("org.antlr:stringtemplate:3.2.1") // Using ST3 because ST4 isn"t supported by JDBI 2
    compile("org.glassfish:javax.json:1.1")
    compile("org.jboss.resteasy:resteasy-jackson2-provider:3.1.2.Final")
    compile("org.jdbi:jdbi:2.78")
    testCompile("junit:junit:4.12")
}
