// Copyright (c) 2026, Oracle and/or its affiliates.

plugins {
    id("fusabase.android-library")
    id("fusabase.android-publish")
}

android {
    namespace = "com.oracle.mobile.fusabase.core"
}

dependencies {
    api("androidx.annotation:annotation:1.9.1")
    api("com.squareup.okhttp3:okhttp:5.1.0")
    api("org.glassfish:jakarta.json:2.0.1")
}
