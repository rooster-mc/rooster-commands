import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.2.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "8.3.3"
}

group = "dev.rooster.commands.demo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.jorel.dev/releases")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:10.0.0")
    implementation("dev.rooster.core:rooster-core:1.0-SNAPSHOT")
    implementation("dev.rooster:rooster-commands:1.0.0")
    implementation("dev.rooster:command-api:1.0.0")
}

tasks {
    runServer {
        minecraftVersion("1.21.5")
        jvmArgs("-Dkotlinx.coroutines.debug=off")
    }
}

tasks.withType<ShadowJar> {
    relocate("dev.jorel.commandapi", "dev.rooster.commands.demo.commandapi")
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
