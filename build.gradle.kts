plugins {
    kotlin("jvm") version "2.2.0"
}

group = "dev.rooster"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("dev.rooster.core:rooster-core:1.0-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
