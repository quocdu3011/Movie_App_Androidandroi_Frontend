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
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MovieApp"

// Application module
include(":app")

// Core modules
include(":core-common")
include(":core-ui")
include(":core-network")
include(":core-database")
include(":core-player")

// Domain & Data modules
include(":domain")
include(":data")

// Feature modules
include(":feature-auth")
include(":feature-home")
include(":feature-detail")
include(":feature-player")
include(":feature-search")
include(":feature-profile")
include(":feature-subscription")