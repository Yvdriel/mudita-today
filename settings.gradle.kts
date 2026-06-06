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
        // Mudita Mindful Design (MMD) e-ink Compose component library.
        // Scoped to com.mudita — the jfrog repo returns an HTML 404 page for any other
        // coordinate, which Gradle fails to parse as a POM ("Already seen doctype") and
        // aborts resolution instead of falling through to the next repo.
        maven {
            url = uri("https://mudita.jfrog.io/artifactory/mmd-release")
            content { includeGroup("com.mudita") }
        }
    }
}

rootProject.name = "Today"
include(":app")
