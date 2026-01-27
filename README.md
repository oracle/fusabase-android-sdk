# OSBDT Android SDK

Oracle Database Backend as a Service (BaaS) SDK for Android.

- Authentication (email/password and federated providers), user profile APIs
- OracleDB document APIs (collections, documents, queries, duality views, transactions, batch)
- Storage APIs (upload/download, metadata, listing)
- Automatic initialization from osbdt-config.json via a Gradle plugin

License: Dual-licensed under UPL 1.0 and Apache 2.0. See LICENSE.txt.

## Requirements

- Android Gradle Plugin: 8.7.0+
- Kotlin: 2.0.21+ (optional; Java supported)
- Java toolchain: 11
- Android compileSdk: 34
- Android minSdk: 26

## Installation

Add the Maven Central repository where the SDK artifacts are published.

settings.gradle.kts
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

build.gradle.kts (module)
```kotlin
dependencies {
    implementation("com.oracle.database.osbdt:osbdt-android-sdk:{version}")
}
```

## Third-Party Dependencies

The OSBDT Android SDK includes the following third-party dependencies:

- `androidx.appcompat:appcompat:1.7.0` - Android AppCompat library
- `androidx.browser:browser:1.8.0` - Android Browser library for custom tabs
- `com.squareup.okhttp3:okhttp:5.1.0` - HTTP client for network operations
- `org.glassfish:jakarta.json:2.0.1` - JSON processing library

These dependencies are automatically included when you add the OSBDT SDK to your project and do not require separate declaration.

## Configuration (osbdt-config.json)

Place an osbdt-config.json file in your app module root (same level as its build.gradle.kts). The OSBDT Gradle plugin reads it and generates res/values/osbdt.generated.xml. These resources are used for auto-initialization.

Example osbdt-config.json
```json
{
  "ords_host": "https://your-ords.example.com/ords/",
  "schema": "YOUR_SCHEMA",
  "app_name": "YourApp",
  "app_type": "WEB",
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
    "clientSecret": "your-client-secret",
    "selfRegistrationProfile": "Default"
  }
}
```

## Apply the OSBDT config Gradle plugin

This plugin converts osbdt-config.json into Android string resources at build time.

build.gradle.kts (app module)
```kotlin
plugins {
    id("com.oracle.database.osbdt.gradle-config")
}
```
build.gradle.kts (project)
```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.oracle.database.osbdt:gradle-config:{version}")
    }
}
```
Alternative (legacy) classpath approach if you cannot use pluginManagement:

build.gradle.kts (app module)
```kotlin
apply(plugin = "com.oracle.database.osbdt.gradle-config")
```

## Quickstart

Auto-initialization:
- The SDK initializes automatically via OsbdtInitProvider using values generated from osbdt-config.json.
- For most apps, no code is required.

Manual initialization (if needed):
```java
import com.oracle.database.osbdt.OsbdtApp;

OsbdtApp app = OsbdtApp.initializeApp(applicationContext); // reads generated resources
```

Initialize with explicit options (advanced):
```java
import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;

OsbdtOptions options = new OsbdtOptions.Builder()
    .setOrdsHost("https://your-ords.example.com/ords/")
    .setProjectId("your-project-id")
    .setAppId("your-app-id")
    .setAppName("YourApp")
    .setAuthType("idcs")
    .setSchema("YOUR_SCHEMA")
    .setApiVersion("v1")
    .setUseSocket(true)
    .build();

OsbdtApp app = OsbdtApp.initializeApp(applicationContext, options);
```

## Usage

OracleDB
```java
import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.oracledb.OsbdtOracledb;
import com.oracle.database.osbdt.oracledb.CollectionReference;
import com.oracle.database.osbdt.oracledb.DocumentReference;

OsbdtApp app = OsbdtApp.getInstance();
OsbdtOracledb db = OsbdtOracledb.getInstance(app);

// References
CollectionReference cities = db.collection("cities");
DocumentReference sf = db.document("cities/sf");

// Queries (example style)
CollectionReference q = db.collection("cities"); // add where/orderBy in your API calls as supported
```

Transactions
```java
import com.oracle.database.osbdt.task.Task;

Task<String> resultTask = db.runTransaction(txn -> {
    // Read/write via txn with your domain-specific APIs
    return "OK";
});
```

Batch
```java
import com.oracle.database.osbdt.oracledb.*;

OsbdtOracledb db = OsbdtOracledb.getInstance(app);
WriteBatch batch = db.batch();

batch.set(db.collection("cities").document("SF"),
    Map.of("name", "San Francisco", "population", 883305));
batch.set(db.collection("cities").document("LA"),
    Map.of("name", "Los Angeles", "population", 3979576));

batch.commit();
```

Auth
```java
import com.oracle.database.osbdt.auth.*;

OsbdtAuth auth = OsbdtAuth.getInstance(app);

// Sign up
auth.createUserWithEmailAndPassword("user@example.com", "password123")
    .addOnSuccessListener(result -> {
        OsbdtUser user = result.getUser();
    });

// Sign in
auth.signInWithEmailAndPassword("user@example.com", "password123")
    .addOnSuccessListener(result -> {
        OsbdtUser user = result.getUser();
    });

// Get current user
OsbdtUser user = auth.getCurrentUser();
String email = user.getEmail();

// Sign out
auth.signOut();
```

Storage
```java
import com.oracle.database.osbdt.storage.*;

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
    android:name="com.oracle.database.osbdt.OsbdtInitProvider"
    android:authorities="${applicationId}.OsbdtInitProvider"
    android:exported="false"
    android:initOrder="100"/>
```

For social login via redirect, add the activity and scheme (if used):
```xml
<activity android:name="com.oracle.database.osbdt.auth.SocialLoginActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data android:scheme="${osbdt_scheme}" />
    </intent-filter>
</activity>
```

## ProGuard / R8

No special rules required by default. If you enable obfuscation, keep public models/APIs you reflect on. See osbdt/proguard-rules.pro for project defaults.

## Testing and Coverage

Instrumented tests and coverage:
```bash
./gradlew connectedDebugAndroidTest
./gradlew :osbdt:jacocoAndroidTestReport
```
Reports are generated under osbdt/build/reports/jacoco.

## Contributing

This project welcomes contributions from the community. Before submitting a pull request, please [review our contribution guide](./CONTRIBUTION.md)

## Security

Please consult the [security guide](./SECURITY.md) for our responsible security vulnerability disclosure process

## Troubleshooting

- osbdt-config.json is not found … Please provide the config file in the app/ directory.
  - Place osbdt-config.json at the app module root (same level as build.gradle.kts).
- Cannot read osbdt-config.json. Please make sure that the file is not corrupted.
  - Validate JSON, ensure the file is readable.
- Social login redirect not working:
  - Add SocialLoginActivity and set osbdt_scheme via resources/config.

## License
Dual-licensed under the UPL 1.0 and Apache 2.0. See LICENSE.txt for details.
