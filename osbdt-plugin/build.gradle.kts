plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("org.glassfish:jakarta.json:2.0.1")
}

gradlePlugin {
    plugins {
        create("gradleConfig") {
            id = "com.oracle.database.osbdt.gradle-config"
            implementationClass = "com.oracle.database.osbdt.OsbdtConfig"
        }
    }
}

// Publishing configuration
publishing {
    publications {
        // This publishes the plugin artifact
        create<MavenPublication>("pluginMaven") {
            groupId = "com.oracle.database.osbdt"
            artifactId = "gradle-config"
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "artifactory"
            url = uri(project.findProperty("artifactoryUrl")?.toString() ?: System.getenv("ARTIFACTORY_URL") ?: error("Missing Artifactory URL"))
            credentials {
                username = project.findProperty("artifactoryUsername")?.toString()
                    ?: System.getenv("ARTIFACTORY_USERNAME")
                            ?: error("Missing Artifactory username")

                password = project.findProperty("artifactoryPassword")?.toString()
                    ?: System.getenv("ARTIFACTORY_PASSWORD")
                            ?: error("Missing Artifactory password")
            }
        }
    }
}
