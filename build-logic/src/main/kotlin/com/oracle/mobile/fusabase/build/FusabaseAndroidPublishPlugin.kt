// Copyright (c) 2026, Oracle and/or its affiliates.

package com.oracle.mobile.fusabase.build

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class FusabaseAndroidPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")
        project.pluginManager.apply("org.jetbrains.dokka")
        project.pluginManager.withPlugin("com.android.library") {
            project.extensions.configure<LibraryExtension> {
                publishing {
                    singleVariant("release")
                }
            }
            val sourcesJar = project.tasks.register<Jar>("fusabaseSourcesJar") {
                archiveClassifier.set("sources")
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                from(project.layout.projectDirectory.dir("src/main/java"))
            }
            val javadocJar = project.tasks.register<Jar>("fusabaseJavadocJar") {
                archiveClassifier.set("javadoc")
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                dependsOn("dokkaHtml")
                from(project.layout.buildDirectory.dir("dokka/html"))
            }
            project.tasks.withType(Jar::class.java).configureEach {
                from(project.rootProject.file("LICENSE.txt")) {
                    into("META-INF")
                    rename { "LICENSE.txt" }
                }
                from(project.rootProject.file("THIRD-PARTY-ATTRIBUTION.txt")) {
                    into("META-INF")
                    rename { "THIRD-PARTY_LICENSE.txt" }
                }
            }
            project.tasks.matching { it.name == "releaseSourcesJar" }.configureEach { enabled = false }
            project.extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("maven") {
                        groupId = "com.oracle.mobile"
                        artifactId = project.name
                        version = project.version.toString()
                        project.afterEvaluate { from(project.components.getByName("release")) }
                        artifact(sourcesJar)
                        artifact(javadocJar)
                        pom.configureFusabasePom(
                            "${project.name} Android SDK",
                            "Oracle Backend for Firebase Android SDK module ${project.name}",
                        )
                    }
                }
            }
            project.configureOptionalArtifactoryRepository()
            project.tasks.matching { it.name == "generateMetadataFileForMavenPublication" }
                .configureEach { dependsOn(sourcesJar); dependsOn(javadocJar) }
        }
    }
}
