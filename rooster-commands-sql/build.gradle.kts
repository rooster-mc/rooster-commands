plugins {
    kotlin("jvm") version "2.2.0"
}

group = "dev.rooster.commands.sql"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    implementation("dev.rooster.core:rooster-core:1.0-SNAPSHOT")
    implementation("dev.rooster:rooster-commands:1.0.0")
    implementation("dev.rooster.db:RoosterDb:1.0-SNAPSHOT")

    implementation("org.jetbrains.exposed:exposed-core:0.49.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.49.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
