plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    withSourcesJar()
    withJavadocJar()
}

val legalFiles = listOf(
    project.rootProject.file("LICENSE.txt"),
    project.rootProject.file("THIRD-PARTY-ATTRIBUTION.txt"),
)

tasks.withType<Jar>().configureEach {
    from(project.rootProject.file("LICENSE.txt")) {
        into("META-INF")
        rename { "LICENSE.txt" }
    }
    from(project.rootProject.file("THIRD-PARTY-ATTRIBUTION.txt")) {
        into("META-INF")
        rename { "THIRD-PARTY_LICENSE.txt" }
    }
}

dependencies {
    implementation("org.glassfish:jakarta.json:2.0.1")
}

gradlePlugin {
    plugins {
        create("gradleConfig") {
            id = "com.oracle.mobile.fusabase-gradle-plugin"
            implementationClass = "com.oracle.mobile.fusabase.FusabaseConfig"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = "com.oracle.mobile"
            artifactId = "gradle-config"
            version = project.version.toString()

            pom.withXml {
                val root = asNode()
                val projectNode = root as groovy.util.Node
                val existing = projectNode.get("packaging") as? groovy.util.NodeList
                existing?.forEach { (it as groovy.util.Node).parent().remove(it) }

                val versionNode = projectNode.get("version") as? groovy.util.NodeList
                val packagingNode = groovy.util.Node(null, "packaging", "jar")
                if (versionNode != null && versionNode.isNotEmpty()) {
                    projectNode.children().add(projectNode.children().indexOf(versionNode[0]) + 1, packagingNode)
                } else {
                    projectNode.children().add(0, packagingNode)
                }
            }

            pom {
                name.set("Fusabase Gradle Config Plugin")
                description.set("Gradle plugin to configure the Fusabase Android SDK")
                url.set("https://github.com/oracle/fusabase-android-sdk")

                licenses {
                    license {
                        name.set("Universal Permissive License (UPL), Version 1.0")
                        url.set("https://oss.oracle.com/licenses/upl")
                        distribution.set("repo")
                    }
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        organization.set("Oracle America, Inc.")
                        organizationUrl.set("http://www.oracle.com")
                    }
                }

                scm {
                    url.set("https://github.com/oracle/fusabase-android-sdk")
                    connection.set("scm:git:https://github.com/oracle/fusabase-android-sdk.git")
                    developerConnection.set("scm:git:ssh://git@github.com:oracle/fusabase-android-sdk.git")
                    tag.set("HEAD")
                }
            }
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
