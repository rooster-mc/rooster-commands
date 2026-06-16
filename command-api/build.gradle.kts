plugins {
    kotlin("jvm") version "2.0.21"
}

group = "dev.cypdashuhn"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.jorel.dev/releases")
}

dependencies {
    implementation(project(":"))
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("dev.jorel:commandapi-bukkit-kotlin:10.0.0")
}

kotlin {
    jvmToolchain(21)
}
