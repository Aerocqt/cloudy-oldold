pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        val ghUser = providers.gradleProperty("gpr.user")
            .orElse(providers.environmentVariable("GPR_USER"))
        val ghToken = providers.gradleProperty("gpr.key")
            .orElse(providers.environmentVariable("GPR_TOKEN"))
        listOf("sesl-androidx", "sesl-material-components-android", "oneui-design").forEach { repo ->
            maven {
                url = uri("https://maven.pkg.github.com/tribalfs/$repo")
                credentials {
                    username = ghUser.get()
                    password = ghToken.get()
                }
            }
        }
    }
}

rootProject.name = "Cloudy"
include(":app")
