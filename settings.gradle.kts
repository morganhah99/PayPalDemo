import java.util.Properties

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
        maven {
            val properties = Properties().apply {
                load(java.io.FileInputStream(rootDir.resolve("local.properties")))
            }
            url = uri("https://cardinalcommerceprod.jfrog.io/artifactory/android")
            credentials {
                username = properties.getProperty("cardinal.username")
                password = properties.getProperty("cardinal.password")
            }
        }
    }
}

rootProject.name = "PayPalDemo"
include(":app")
