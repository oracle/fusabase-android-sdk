pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://artifacthub-iad.oci.oraclecorp.com/parfaitsuite-release")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://artifacthub-iad.oci.oraclecorp.com/parfaitsuite-release")
    }
}

rootProject.name = "OSBDT"
include(":osbdt")
include(":osbdt-plugin")
