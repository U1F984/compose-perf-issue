plugins {
    kotlin("jvm") version "1.8.0"
    application
    id("org.jetbrains.compose") version "1.3.0"
}

group = "cc.tietz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.compose.runtime:runtime:1.3.0")
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