# Oracle Backend for Firebase Android SDK Guide for Coding Agents

This repository contains the Oracle Backend for Firebase Android SDK. The product name is **Oracle Backend for Firebase**. The developer-facing Android package prefix is **`com.oracle.mobile.fusabase`**.

Use this file as background context when you are scanning the SDK or generating Android application code that consumes it.

## Canonical Imports

Use only the public SDK packages:

```java
import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.auth.FusabaseAuth;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.storage.Storage;
import com.oracle.mobile.fusabase.task.Task;
```

Do not generate application code that imports internal source paths or repository-only implementation packages such as `core`, `http`, `logger`, `models`, or `utils` unless a public API explicitly requires one of their exposed types.

## SDK Surface

The Android SDK is published from the `:fusabase` library module and exposes public APIs under:

- `com.oracle.mobile.fusabase`: app initialization, options, timestamps, and top-level exceptions.
- `com.oracle.mobile.fusabase.auth`: auth entrypoints, users, providers, credentials, and token/listener APIs.
- `com.oracle.mobile.fusabase.oracledb`: collections, documents, queries, listeners, transactions, write batches, aggregates, vector search, and duality views.
- `com.oracle.mobile.fusabase.storage`: storage clients, references, uploads, downloads, metadata, listing, and task snapshots.
- `com.oracle.mobile.fusabase.task`: the SDK’s own task/listener abstractions used by auth, database, and storage.

## Configuration

Android apps typically initialize from `fusabase_config.json`, which is merged into app resources and read automatically by `FusabaseInitProvider`.

High-level configuration fields include:

- `ords_host`
- `project_id`
- `app_id`
- `app_name`
- `schema`
- `auth_type`
- `auth_id`
- `objs_type`
- `storage_bucket`
- `api_version`
- `use_socket`
- `enable_logging`
- `upload_chunk_size`
- `long_polling_interval`
- `allows_self_signed_certificates`

Typical setup:

```java
FusabaseApp app = FusabaseApp.getInstance();
```

Manual setup is also supported:

```java
FusabaseOptions options = new FusabaseOptions.Builder()
    .setOrdsHost("https://example/ords/schema/")
    .setProjectId("project-id")
    .setAppId("app-id")
    .setSchema("schema")
    .build();

FusabaseApp app = FusabaseApp.initializeApp(context, options);
```

## Android Usage Shape

- The SDK is package-based, not module-subpath-based.
- Most async APIs return `com.oracle.mobile.fusabase.task.Task<T>`.
- Default instances are commonly accessed through `FusabaseApp.getInstance()`, `FusabaseAuth.getInstance()`, `FusabaseOracledb.getInstance()`, and `Storage.getInstance()`.
- Automatic initialization is common on Android because `FusabaseInitProvider` reads config during app startup.

## Short Examples

### App

```java
import com.oracle.mobile.fusabase.FusabaseApp;

FusabaseApp app = FusabaseApp.getInstance();
```

### Auth

```java
import com.oracle.mobile.fusabase.auth.FusabaseAuth;

FusabaseAuth auth = FusabaseAuth.getInstance();
auth.addAuthStateListener(fusabaseAuth -> {
    if (fusabaseAuth.getCurrentUser() != null) {
        System.out.println(fusabaseAuth.getCurrentUser().getEmail());
    }
});

auth.signInWithEmailAndPassword(email, password);
```

### OracleDB

```java
import com.oracle.mobile.fusabase.oracledb.CollectionReference;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;

FusabaseOracledb db = FusabaseOracledb.getInstance();
CollectionReference recipes = db.collection("recipes");
recipes.addDocument(new java.util.HashMap<String, Object>() {{
    put("title", "Tea");
    put("category", "Drinks");
}});
```

### Storage

```java
import com.oracle.mobile.fusabase.storage.Storage;
import com.oracle.mobile.fusabase.storage.StorageReference;

Storage storage = Storage.getInstance();
StorageReference imageRef = storage.getReference("recipes/tea.png");
imageRef.putBytes(fileBytes);
```

## More Detail

Use these docs when you need deeper module-specific context:

- `agent_docs/configuration.md`: config resource flow, `FusabaseOptions`, and initialization patterns.
- `agent_docs/core.md`: `FusabaseApp`, top-level types, and Android app bootstrap behavior.
- `agent_docs/auth.md`: auth entrypoints, users, listeners, provider flows, and token APIs.
- `agent_docs/oracledb.md`: references, queries, listeners, writes, transactions, aggregates, vector search, and duality views.
- `agent_docs/storage.md`: storage clients, references, uploads, downloads, metadata, list APIs, and task snapshots.
- `agent_docs/tasks.md`: the SDK task model and listener types.
- `agent_docs/api-reference.md`: how to generate Dokka output for the Android SDK and where it is written.

## API Reference

Generate local API docs with:

```bash
./gradlew :fusabase:dokkaHtml
```

That task generates the Dokka reference for the Android library module. See `agent_docs/api-reference.md` for details.
