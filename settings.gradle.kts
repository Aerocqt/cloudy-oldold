pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // tribalfs SESL / OneUI-design are published via JitPack.
        // Verify the latest tag against each repo's README before building.
        maven("https://jitpack.io")
    }
}
rootProject.name = "Cloudy"
include(":app")
