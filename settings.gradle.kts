val jitpackAuthToken = providers.environmentVariable("JITPACK_AUTH_TOKEN")
    .orElse(providers.gradleProperty("JITPACK_AUTH_TOKEN"))
    .orNull

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            if (!jitpackAuthToken.isNullOrBlank()) {
                credentials {
                    username = jitpackAuthToken
                }
            }
        }
    }
}
rootProject.name = "Kujaku"
include(":app")
include(":animatedbottombar")
