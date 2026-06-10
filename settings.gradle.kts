pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "shadow-village-defense"

include(":core")

// The :app module needs the Android SDK at configuration time. Skip it on
// bare-JVM machines so `gradlew :core:test` always works.
val sdkAvailable = file("local.properties").let { it.exists() && it.readText().contains("sdk.dir") } ||
    System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null
if (sdkAvailable) {
    include(":app")
} else {
    logger.lifecycle("Android SDK not found - building :core only. Run scripts/setup-android-sdk.sh to enable :app.")
}
