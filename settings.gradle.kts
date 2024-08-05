pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        jcenter()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        flatDir {
            dirs("${rootProject.projectDir}/demo/libs")
        }
        mavenCentral()
        maven{
            url = uri("https://jitpack.io")
        }
        jcenter()
    }
}

rootProject.name = "foundation.algorand.demo"
include(":demo")
include(":liquid")
