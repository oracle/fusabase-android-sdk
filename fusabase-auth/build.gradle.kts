// Copyright (c) 2026, Oracle and/or its affiliates.

plugins {
    id("fusabase.android-library")
    id("fusabase.android-publish")
}

android {
    namespace = "com.oracle.mobile.fusabase.auth"
}

dependencies {
    api(project(":fusabase-common"))
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.browser:browser:1.8.0")
    api("org.glassfish:jakarta.json:2.0.1")
}
