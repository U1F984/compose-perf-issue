plugins {
    kotlin("jvm") version "1.7.20"
    application
    id("org.jetbrains.compose") version "1.2.2"
}

group = "cc.tietz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.compose.runtime:runtime:1.2.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}