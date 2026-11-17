// Copyright (c) 2026, Oracle and/or its affiliates.

plugins {
    id("fusabase.android-library")
    id("fusabase.android-publish")
}

android {
    namespace = "com.oracle.mobile.fusabase.oracledb"
}

dependencies {
    api(project(":fusabase-common"))
    api("org.glassfish:jakarta.json:2.0.1")
    implementation("androidx.lifecycle:lifecycle-common:2.8.7")
}
