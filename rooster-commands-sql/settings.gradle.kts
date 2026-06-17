plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "RoosterCommandsSql"

includeBuild("../../rooster-core")
includeBuild("../")
includeBuild("../../rooster-sql")
