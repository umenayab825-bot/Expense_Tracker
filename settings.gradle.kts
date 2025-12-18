// settings.gradle.kts

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // JitPack for MPAndroidChart dependencies/plugins
        maven("https://jitpack.io") // <--- YAHAN ADD KAR DIYA HAI
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack repository for MPAndroidChart library
        maven("https://jitpack.io") // <--- YAHAN ALREADY MAUJOOD THA
    }
}

rootProject.name = "Expense Tracker"
include(":app")