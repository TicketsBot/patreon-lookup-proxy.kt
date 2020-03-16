plugins {
    kotlin("jvm") version "1.3.61"
}

group = "net.ticketsbot.patreonproxy"
version = "1.0-SNAPSHOT"

var ktor_version = "1.3.2"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.patreon:patreon:0.4.2")
    implementation("org.json:json:20190722")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}