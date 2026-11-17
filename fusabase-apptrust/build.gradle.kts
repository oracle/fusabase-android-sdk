// Copyright (c) 2026, Oracle and/or its affiliates.

plugins {
    id("fusabase.android-library")
    id("fusabase.android-publish")
}

android {
    namespace = "com.oracle.mobile.fusabase.apptrust"
}

dependencies {
    api(project(":fusabase-common"))
    implementation("com.google.android.play:integrity:1.4.0")
    api("org.glassfish:jakarta.json:2.0.1")
}
