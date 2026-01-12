plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("oracle.parfait") version "2.4.29"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("org.glassfish:jakarta.json:2.0.1")
    compileOnly("com.android.tools.build:gradle:8.3.2")
}

gradlePlugin {
    plugins {
        create("osbdtConfigPlugin") {
            id = "com.oracle.database.osbdt.osbdt-config-plugin"
            implementationClass = "com.oracle.database.OsbdtConfig"
        }
    }
}

// Publishing configuration
publishing {
    publications {
        // This publishes the plugin artifact
        create<MavenPublication>("pluginMaven") {
            groupId = "com.oracle.database.osbdt"
            artifactId = "osbdt-config-plugin"
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "artifactory"
            url = uri("https://artifacthub-iad.oci.oraclecorp.com/artifactory/dst-maven-dev-local")
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
