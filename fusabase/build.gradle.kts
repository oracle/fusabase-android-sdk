// Copyright (c) 2015, 2025, Oracle and/or its affiliates.

//-----------------------------------------------------------------------------
//
// This software is dual-licensed to you under the Universal Permissive License
// (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl and Apache License
// 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
// either license.
//
// If you elect to accept the software under the Apache License, Version 2.0,
// the following applies:
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//-----------------------------------------------------------------------------

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("jacoco")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
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

android {
    sourceSets {
        getByName("main") {
            assets.srcDir(project.layout.projectDirectory.dir("src/main/assets"))
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
    }
}

android {
    namespace = "com.oracle.mobile.fusabase"
    compileSdk = 34


    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        getByName("debug") {
            enableAndroidTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += setOf(
                "META-INF/LICENSE.txt",
                "META-INF/THIRD-PARTY_LICENSE.txt",
            )
        }
    }
}


tasks.register<JacocoReport>("jacocoAndroidTestReport") {
    group = "Reporting"
    description = "Generate Jacoco coverage reports for Android instrumentation tests."

    dependsOn(
        "connectedDebugAndroidTest",
        "compileDebugJavaWithJavac",
        "compileDebugKotlin"
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val buildDirFile = layout.buildDirectory.get().asFile

    val javaClasses = fileTree(buildDirFile.resolve("intermediates/javac/debug/classes")) {
        exclude(fileFilter)
    }
    val kotlinClasses = fileTree(buildDirFile.resolve("tmp/kotlin-classes/debug")) {
        exclude(fileFilter)
    }

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    classDirectories.setFrom(files(javaClasses, kotlinClasses))

    val coverageFile = fileTree(buildDirFile.resolve("outputs/code_coverage/debugAndroidTest/connected/")) {
        include("**/*.ec")
    }
    executionData.setFrom(coverageFile)

    doFirst {
        if (coverageFile.isEmpty) {
            throw GradleException(
                "No coverage.ec file found. Run ./gradlew connectedDebugAndroidTest first."
            )
        }
    }
}

dependencies {

    // Core AndroidX & Jetpack
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Security & browser
    implementation("androidx.browser:browser:1.8.0")

    // Networking & JSON
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("org.glassfish:jakarta.json:2.0.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(android.sourceSets["main"].java.srcDirs)
    from("src/main/kotlin")
    from(project.rootProject.file("LICENSE.txt")) {
        into("META-INF")
        rename { "LICENSE.txt" }
    }
    from(project.rootProject.file("THIRD-PARTY-ATTRIBUTION.txt")) {
        into("META-INF")
        rename { "THIRD-PARTY_LICENSE.txt" }
    }
}

tasks.matching { it.name == "releaseSourcesJar" }.configureEach {
    (this as org.gradle.jvm.tasks.Jar).duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    (this as org.gradle.jvm.tasks.Jar).from(project.rootProject.file("LICENSE.txt")) {
        into("META-INF")
        rename { "LICENSE.txt" }
    }
    (this as org.gradle.jvm.tasks.Jar).from(project.rootProject.file("THIRD-PARTY-ATTRIBUTION.txt")) {
        into("META-INF")
        rename { "THIRD-PARTY_LICENSE.txt" }
    }
}

tasks.matching { it.name == "releaseSourcesJar" }.configureEach {
    enabled = false
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(tasks.named("dokkaHtml"))
    from(tasks.named("dokkaHtml").map { (it as org.jetbrains.dokka.gradle.DokkaTask).outputDirectory })
    from(project.rootProject.file("LICENSE.txt")) {
        into("META-INF")
        rename { "LICENSE.txt" }
    }
    from(project.rootProject.file("THIRD-PARTY-ATTRIBUTION.txt")) {
        into("META-INF")
        rename { "THIRD-PARTY_LICENSE.txt" }
    }
}

tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml") {
    doFirst {
        delete(outputDirectory.get().asFile)
    }

    moduleName.set("Oracle® Backend for Firebase Android SDK Reference, Release 26.1.0")
    moduleVersion.set("Release 26.1.0 (G48185-03)")

    dokkaSourceSets.configureEach {
        pluginsMapConfiguration.set(
            mapOf(
                "org.jetbrains.dokka.base.DokkaBase" to
                    "{\"footerMessage\":\"<div style='text-align:center;'>Copyright &copy; 2026, Oracle and/or its affiliates.</div>\"}"
            )
        )

        perPackageOption {
            matchingRegex.set("com\\.oracle\\.mobile\\.fusabase\\.(core|http|logger|models|utils)(\\..*)?")
            suppress.set(true)
        }

        suppressedFiles.from(
            fileTree("src/main/java") {
                include(
                    "com/oracle/mobile/fusabase/core/**",
                    "com/oracle/mobile/fusabase/http/**",
                    "com/oracle/mobile/fusabase/logger/**",
                    "com/oracle/mobile/fusabase/models/**",
                    "com/oracle/mobile/fusabase/utils/**",
                )
            }
        )

        suppressedFiles.from(file("src/main/java/com/oracle/mobile/fusabase/task/Tasks.java"))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.oracle.mobile"
            artifactId = "fusabase"
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }

            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                name.set("Fusabase Android SDK")
                description.set("Oracle Backend for Firebase SDK for Android")
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
            url = uri(project.findProperty("artifactoryUrl")?.toString() ?: System.getenv("ARTIFACTORY_URL") ?: error("Missing artifactoryUrl"))

            credentials {
                username = project.findProperty("artifactoryUsername")?.toString() ?: System.getenv("ARTIFACTORY_USERNAME") ?: error("Missing artifactoryUsername")
                password = project.findProperty("artifactoryPassword")?.toString() ?: System.getenv("ARTIFACTORY_PASSWORD") ?: error("Missing artifactoryPassword")
            }
        }
    }
}

tasks.matching { it.name == "generateMetadataFileForMavenPublication" }.configureEach {
    dependsOn(sourcesJar)
    dependsOn(javadocJar)
}
