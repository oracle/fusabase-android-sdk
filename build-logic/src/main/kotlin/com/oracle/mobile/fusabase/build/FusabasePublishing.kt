// Copyright (c) 2026, Oracle and/or its affiliates.

package com.oracle.mobile.fusabase.build

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom
import org.gradle.kotlin.dsl.configure

internal fun MavenPom.configureFusabasePom(artifactName: String, descriptionText: String) {
    name.set(artifactName)
    description.set(descriptionText)
    url.set("https://github.com/oracle/fusabase-android-sdk")
    licenses { license {
        name.set("Universal Permissive License (UPL), Version 1.0")
        url.set("https://oss.oracle.com/licenses/upl")
        distribution.set("repo")
    } }
    developers { developer {
        organization.set("Oracle America, Inc.")
        organizationUrl.set("http://www.oracle.com")
    } }
    scm {
        url.set("https://github.com/oracle/fusabase-android-sdk")
        connection.set("scm:git:https://github.com/oracle/fusabase-android-sdk.git")
        developerConnection.set("scm:git:ssh://git@github.com:oracle/fusabase-android-sdk.git")
        tag.set("HEAD")
    }
}

internal fun Project.configureOptionalArtifactoryRepository() {
    val artifactoryUrl = providers.gradleProperty("artifactoryUrl")
        .orElse(providers.environmentVariable("ARTIFACTORY_URL")).orNull
    val artifactoryUsername = providers.gradleProperty("artifactoryUsername")
        .orElse(providers.environmentVariable("ARTIFACTORY_USERNAME")).orNull
    val artifactoryPassword = providers.gradleProperty("artifactoryPassword")
        .orElse(providers.environmentVariable("ARTIFACTORY_PASSWORD")).orNull
    if (artifactoryUrl.isNullOrBlank() || artifactoryUsername.isNullOrBlank()
        || artifactoryPassword.isNullOrBlank()) return
    extensions.configure<org.gradle.api.publish.PublishingExtension> {
        repositories { maven {
            name = "artifactory"
            url = uri(artifactoryUrl)
            credentials { username = artifactoryUsername; password = artifactoryPassword }
        } }
    }
}
