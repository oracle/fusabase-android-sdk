# Oracle® Backend for Firebase (Fusabase) Android SDK

An Android SDK for Oracle Backend for Firebase (Fusabase) that provides authentication, document database, storage, and vector search capabilities.

- Authentication (email/password and federated providers), user profile APIs
- OracleDB document APIs (collections, documents, queries, duality views, transactions, batch)
- Vector search (dense and sparse embeddings, similarity queries)
- Storage APIs (upload/download, metadata, listing)
- Automatic initialization from `fusabase-config.json` via a Gradle plugin

This Oracle Backend for Firebase Android SDK follows the Firebase design, API patterns, and rules-based authorization model. Its SDK interfaces are designed to mirror the Firebase SDK interfaces so developers can move across backends with minimal changes. This Oracle Backend for Firebase Android SDK is a distinct Oracle offering.

## Requirements

- Android Gradle Plugin: 8.7.0+
- Kotlin: 2.0.21+ (optional; Java supported)
- Java toolchain: 11
- Android `compileSdk`: 34
- Android `minSdk`: 26

## Installation

Add the Maven Central repository where the SDK artifacts are published.

`settings.gradle.kts`
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

Add the library dependency to your app/library module:

`build.gradle.kts` (module)
```kotlin
dependencies {
    implementation("com.oracle.mobile:fusabase:{version}")
}
```

Third-party dependencies pulled in transitively:

- `androidx.appcompat:appcompat:1.7.0`
- `androidx.browser:browser:1.8.0` (Custom Tabs for federated auth)
- `com.squareup.okhttp3:okhttp:5.1.0`
- `org.glassfish:jakarta.json:2.0.1`

## Configuration (`fusabase-config.json`)

Place a `fusabase-config.json` file in your app module root (same level as its `build.gradle.kts`). The Fusabase Gradle plugin reads it and generates `res/values/fusabase.generated.xml`. These resources are used for auto-initialization.

Example `fusabase-config.json`:
```json
{
  "ords_host": "https://your-ords.example.com/ords/",
  "schema": "YOUR_SCHEMA",
  "app_name": "YourApp",
  "app_id": "your-app-id",
  "objs_type": "oci-objects",
  "storage_bucket": "your-bucket",
  "auth_type": "idcs",
  "auth_id": "your-auth-id",
  "project_id": "your-project-id",
  "api_version": 2,
  "useSocket": true,
  "upload_chunk_size": 1048576,
  "long_polling_interval": 3000,
  "idcs_options": {
    "domain_url": "https://idcs-guid.oraclecloud.com",
    "clientId": "your-client-id",
    "clientSecret": "your-client-secret"
  }
}
```

`auth_type` accepts `base`, `idcs`, or `ldap`.

## Apply the Fusabase config Gradle plugin

This plugin converts `fusabase-config.json` into Android string resources at build time.

`build.gradle.kts` (app module)
```kotlin
plugins {
    id("com.oracle.mobile.fusabase-gradle-plugin")
}
```

`build.gradle.kts` (project)
```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.oracle.mobile:fusabase-gradle-plugin:{version}")
    }
}
```

Alternative (legacy) classpath approach if you cannot use `pluginManagement`:

`build.gradle.kts` (app module)
```kotlin
apply(plugin = "com.oracle.mobile.fusabase-gradle-plugin")
```

## Quickstart

Auto-initialization:
- The SDK initializes automatically via `FusabaseInitProvider` using values generated from `fusabase-config.json`.
- For most apps, no code is required.

Manual initialization (if needed):
```java
import com.oracle.mobile.fusabase.FusabaseApp;

FusabaseApp app = FusabaseApp.initializeApp(applicationContext); // reads generated resources
```

Initialize with explicit options (advanced):
```java
import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;

FusabaseOptions options = new FusabaseOptions.Builder()
    .setOrdsHost("https://your-ords.example.com/ords/")
    .setProjectId("your-project-id")
    .setAppId("your-app-id")
    .setAppName("YourApp")
    .setAuthType("idcs")
    .setSchema("YOUR_SCHEMA")
    .setApiVersion("v1")
    .setUseSocket(true)
    .build();

FusabaseApp app = FusabaseApp.initializeApp(applicationContext, options);
```

## Usage

### Authentication

```java
import com.oracle.mobile.fusabase.auth.*;

FusabaseAuth auth = FusabaseAuth.getInstance(app);

// Sign up
auth.createUserWithEmailAndPassword("user@example.com", "password123!")
    .addOnSuccessListener(result -> {
        FusabaseUser user = result.getUser();
    });

// Sign in
auth.signInWithEmailAndPassword("user@example.com", "password123!")
    .addOnSuccessListener(result -> {
        FusabaseUser user = result.getUser();
    });

// Observe auth state
auth.addAuthStateListener(authInstance -> {
    FusabaseUser current = authInstance.getCurrentUser();
    if (current != null) {
        // signed in
    } else {
        // signed out
    }
});

// Current user
FusabaseUser user = auth.getCurrentUser();

// Sign out
auth.signOut();
```

### Database (OracleDB)

```java
import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.oracledb.CollectionReference;
import com.oracle.mobile.fusabase.oracledb.DocumentReference;
import com.oracle.mobile.fusabase.oracledb.Query;

FusabaseApp app = FusabaseApp.getInstance();
FusabaseOracledb db = FusabaseOracledb.getInstance(app);

// References
CollectionReference posts = db.collection("posts");
DocumentReference postRef = db.document("posts/post-123");

// Query: filter + order + limit
Query q = posts
    .whereEqualTo("published", true)
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .limit(10);
```

### Vector Search

The SDK supports similarity search over dense and sparse embeddings. Use `Query.findNearest(...)`.

```java
import com.oracle.mobile.fusabase.oracledb.*;
import java.util.Arrays;

// Dense vector similarity search
FindNearestQuery denseQ = FindNearestQuery.dense(
    Arrays.asList(0.22, 0.93, -0.10)
);
FindNearestOptions denseOpts = new FindNearestOptions.Builder()
    .metric(VectorMetric.COSINE)
    .topK(10)
    .build();
Query denseQuery = db.collection("documents").findNearest("embedding", denseQ, denseOpts);

// Sparse vector similarity search
SparseVector sparse = VectorValue.sparseVector(
    /* dimension */ 1000,
    /* indices   */ Arrays.asList(2, 7, 900),
    /* values    */ Arrays.asList(0.9, 0.3, 0.5)
);
FindNearestQuery sparseQ = FindNearestQuery.sparse(sparse);
FindNearestOptions sparseOpts = new FindNearestOptions.Builder()
    .metric(VectorMetric.DOT)
    .topK(5)
    .build();
Query sparseQuery = db.collection("documents").findNearest("embedding", sparseQ, sparseOpts);
```

Supported metrics: `VectorMetric.COSINE`, `VectorMetric.EUCLIDEAN`, `VectorMetric.DOT`.

### Transactions

```java
import com.oracle.mobile.fusabase.task.Task;

Task<String> resultTask = db.runTransaction(txn -> {
    // Read/write via txn with your domain-specific APIs
    return "OK";
});
```

### Batch

```java
import com.oracle.mobile.fusabase.oracledb.*;

FusabaseOracledb db = FusabaseOracledb.getInstance(app);
WriteBatch batch = db.batch();

batch.set(db.collection("cities").document("SF"),
    Map.of("name", "San Francisco", "population", 883305));
batch.set(db.collection("cities").document("LA"),
    Map.of("name", "Los Angeles", "population", 3979576));

batch.commit();
```

### Storage

```java
import com.oracle.mobile.fusabase.storage.*;

Storage storage = Storage.getInstance(app);
StorageReference fileRef = storage.getReference().child("images/photo.jpg");

// Upload
byte[] data = "Hello World!".getBytes();
fileRef.putBytes(data).addOnSuccessListener(snapshot -> {
    // Upload successful
});

// Download
fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
    // Process downloaded data
});

// Get download URL
fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
    // Use download URL
});

// List files
storage.getReference().child("images/").listAll()
    .addOnSuccessListener(result -> {
        for (StorageReference item : result.getItems()) {
            // Process files
        }
    });
```

## Android Manifest

The SDK auto-registers a content provider:
```xml
<provider
    android:name="com.oracle.mobile.fusabase.FusabaseInitProvider"
    android:authorities="${applicationId}.FusabaseInitProvider"
    android:exported="false"
    android:initOrder="100"/>
```

For social login via redirect, add the activity and scheme (if used):
```xml
<activity android:name="com.oracle.mobile.fusabase.auth.SocialLoginActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data android:scheme="${fusabase_scheme}" />
    </intent-filter>
</activity>
```

## ProGuard / R8

No special rules required by default. If you enable obfuscation, keep public models/APIs you reflect on. See `fusabase/proguard-rules.pro` for project defaults.

## Documentation

API documentation is generated with Dokka:

```bash
./gradlew :fusabase:dokkaHtml
```

Output is written under `fusabase/build/dokka/html/`.

## Building the Project

Build a release AAR:

```bash
./gradlew :fusabase:assembleRelease
```

Publish to your local Maven repository for use in another project on this machine:

```bash
./gradlew :fusabase:publishToMavenLocal
```

## Running Tests

Unit and instrumented tests:

```bash
./gradlew :fusabase:test
./gradlew :fusabase:connectedDebugAndroidTest
```

Coverage report:

```bash
./gradlew :fusabase:jacocoAndroidTestReport
```

Reports are generated under `fusabase/build/reports/jacoco`.

## Troubleshooting

- `fusabase-config.json is not found ... Please provide the config file in the app/ directory.`
  - Place `fusabase-config.json` at the app module root (same level as `build.gradle.kts`).
- `Cannot read fusabase-config.json. Please make sure that the file is not corrupted.`
  - Validate JSON, ensure the file is readable.
- Social login redirect not working:
  - Add `SocialLoginActivity` and set `fusabase_scheme` via resources/config.
 
## Trademarks

Firebase is a trademark of Google LLC.  Use of the Firebase name here is solely to describe the design patterns and SDK interfaces that Oracle Backend for Firebase follows for developer familiarity and ease of migration; it does not imply any affiliation with or endorsement by Google.

## Contributing

This project welcomes contributions from the community. Before submitting a pull request, please [review our contribution guide](./CONTRIBUTING.md).

## Security

Please consult the [security guide](./SECURITY.md) for our responsible security vulnerability disclosure process.

## License

Copyright (c) 2015, 2026, Oracle and/or its affiliates.

This software is dual-licensed to you under the Universal Permissive License
(UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl and Apache License
2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
either license.

If you elect to accept the software under the Apache License, Version 2.0,
the following applies:

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
