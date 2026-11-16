// Copyright (c) 2026, Oracle and/or its affiliates.

package com.oracle.mobile.fusabase.build

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class FusabaseAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.android.library")

        project.extensions.configure<LibraryExtension> {
            compileSdk = 34
            defaultConfig {
                minSdk = 26
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    pickFirsts += setOf("META-INF/LICENSE.txt", "META-INF/THIRD-PARTY_LICENSE.txt")
                }
            }
        }
        project.dependencies.add("testImplementation", "junit:junit:4.13.2")
        project.dependencies.add("androidTestImplementation", "androidx.test.ext:junit:1.1.5")
        project.dependencies.add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.5.1")
    }
}
