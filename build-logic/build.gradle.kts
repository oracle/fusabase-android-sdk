// Copyright (c) 2026, Oracle and/or its affiliates.

plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.7.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20")
}

gradlePlugin {
    plugins {
        register("fusabaseAndroidLibrary") {
            id = "fusabase.android-library"
            implementationClass = "com.oracle.mobile.fusabase.build.FusabaseAndroidLibraryPlugin"
        }
        register("fusabaseAndroidPublish") {
            id = "fusabase.android-publish"
            implementationClass = "com.oracle.mobile.fusabase.build.FusabaseAndroidPublishPlugin"
        }
    }
}
