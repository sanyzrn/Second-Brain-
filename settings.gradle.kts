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

rootProject.name = "SecondBrain"

include(":app")
include(":core:designsystem")
include(":core:security")
include(":core:database")
include(":core:data")
include(":core:ai")
include(":core:reminders")
include(":core:calendar")
include(":feature:inbox")
include(":feature:reminders")
include(":feature:habits")
include(":feature:finance")
include(":feature:medicine")
include(":feature:itemdetail")
include(":feature:project")
include(":feature:timeline")
include(":feature:search")
include(":feature:settings")
include(":feature:onboarding")
