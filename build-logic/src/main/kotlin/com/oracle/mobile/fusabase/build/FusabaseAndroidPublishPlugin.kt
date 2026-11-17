// Copyright (c) 2026, Oracle and/or its affiliates.

package com.oracle.mobile.fusabase.build

import com.android.build.api.dsl.LibraryExtension
import groovy.json.JsonOutput
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaTask

class FusabaseAndroidPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")
        project.pluginManager.apply("org.jetbrains.dokka")
        project.dependencies.add(
            "dokkaHtmlPlugin",
            "org.jetbrains.dokka:kotlin-as-java-plugin:1.9.20",
        )
        project.tasks.withType(DokkaTask::class.java).configureEach {
            doFirst { project.delete(outputDirectory.get().asFile) }
            moduleName.set("Oracle Backend with Firebase APIs Android SDK Reference, Release 26.3.1")
            moduleVersion.set("Release 26.3.1 (G62185-02)")
            val templatesDirectory = project.rootProject.file("build-logic/dokka/templates")
            inputs.dir(templatesDirectory)
            pluginsMapConfiguration.set(
                mapOf(
                    "org.jetbrains.dokka.base.DokkaBase" to
                        JsonOutput.toJson(
                            mapOf(
                                "footerMessage" to "Copyright &copy; 2026, Oracle and/or its affiliates.",
                                "templatesDir" to templatesDirectory.absolutePath,
                            )
                        ),
                )
            )
            // These Java-only Android modules do not have Kotlin source sets for Dokka to discover.
            dokkaSourceSets.maybeCreate("main").apply {
                sourceRoots.from(project.layout.projectDirectory.dir("src/main/java"))
                classpath.from(project.provider {
                    project.extensions.getByType(com.android.build.gradle.BaseExtension::class.java).bootClasspath
                })
                classpath.from(project.configurations.named("releaseCompileClasspath").map {
                    it.incoming.artifactView {
                        attributes.attribute(
                            org.gradle.api.attributes.Attribute.of("artifactType", String::class.java),
                            "android-classes-jar",
                        )
                    }.files
                })
            }
            val javaSources = project.fileTree("src/main/java") { include("**/*.java") }
            inputs.files(javaSources)
            val restrictedSources = project.provider { dokkaRestrictedSources(javaSources.files) }
            dokkaSourceSets.configureEach {
                suppressedFiles.from(restrictedSources)
                perPackageOption {
                    matchingRegex.set("com\\.oracle\\.mobile\\.fusabase\\.(core|http|logger|models|utils)(\\..*)?")
                    suppress.set(true)
                }
                suppressedFiles.from(
                    project.fileTree("src/main/java") {
                        include(
                            "com/oracle/mobile/fusabase/core/**",
                            "com/oracle/mobile/fusabase/http/**",
                            "com/oracle/mobile/fusabase/logger/**",
                            "com/oracle/mobile/fusabase/models/**",
                            "com/oracle/mobile/fusabase/utils/**",
                            "com/oracle/mobile/fusabase/task/Tasks.java",
                        )
                    }
                )
            }
        }
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
