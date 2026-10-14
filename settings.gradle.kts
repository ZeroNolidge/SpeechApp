pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // Not FAIL_ON_PROJECT_REPOS: the Kotlin/Wasm build adds its own repositories to download
    // Node.js and Yarn for the web version.
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "SpeechApp"
include(":app")
