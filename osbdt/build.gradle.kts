plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("jacoco")
    id("maven-publish")
    id("signing")
    id("oracle.parfait.android") version "2.4.29"
}

android {
    namespace = "com.oracle.database.osbdt"
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
        }
    }
}

tasks.register<JacocoReport>("jacocoAndroidTestReport") {
    group = "Reporting"
    description = "Generate Jacoco coverage reports for Android instrumentation tests."

    // ✅ Ensure dependent tasks run first
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

    // Auto-pick coverage.ec file
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
    testImplementation("org.robolectric:robolectric:4.8")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("org.mockito:mockito-android:4.0.0")

}

// Configure Maven publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.oracle.database.osbdt"
            artifactId = "osbdt-android-sdk"
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Oracle Database OSBDT Android SDK")
                description.set("Oracle Database Backend as a Service (BaaS) Android SDK")
            }
        }
    }

    repositories {
        maven {
            name = "artifactory"
            url = uri("https://artifacthub-iad.oci.oraclecorp.com/artifactory/dst-maven-release-local")

            credentials {
                username = project.findProperty("artifactoryUsername")?.toString() ?: System.getenv("ARTIFACTORY_USERNAME")
                password = project.findProperty("artifactoryPassword")?.toString() ?: System.getenv("ARTIFACTORY_PASSWORD")
            }
        }
    }
}
