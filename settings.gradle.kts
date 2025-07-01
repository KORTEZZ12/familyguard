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
    }
    plugins {
        // Здесь объявляем версии плагинов один раз
        id("com.android.application") version "8.1.4"
        id("org.jetbrains.kotlin.android") version "1.9.24"
        id("jacoco")
    }
}

    dependencyResolutionManagement {
        repositories {
            google()
            mavenCentral()
        }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TestJava"
include(":app")
 