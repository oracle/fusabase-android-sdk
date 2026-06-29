# Configuration

Use `FusabaseOptions` and `FusabaseApp` from `com.oracle.mobile.fusabase` to configure the Android SDK.

## Default Android Flow

The Android SDK supports automatic initialization through `FusabaseInitProvider`. In the common case, your app includes `fusabase-config.json`, the build merges config into resources, and the provider initializes the default app at startup.

Typical consumption pattern:

```java
FusabaseApp app = FusabaseApp.getInstance();
```

## Manual Configuration

Use `FusabaseOptions.Builder` when you need to initialize an app explicitly:

```java
import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;

FusabaseOptions options = new FusabaseOptions.Builder()
    .setOrdsHost("https://example/ords/schema/")
    .setProjectId("project-id")
    .setAppName("MyApp")
    .setAppId("app-id")
    .setSchema("schema")
    .setAuthType("idcs")
    .setAuthId("auth-id")
    .setIdcsDomainURL("https://example.identity.oracle.com")
    .setObjectsType("dbfs")
    .setStorageBucket("bucket-name")
    .build();

FusabaseApp app = FusabaseApp.initializeApp(context, options);
```

## High-Level Fields

Important option accessors include:

- `getOrdsHost()`
- `getProjectId()`
- `getAppId()`
- `getAppName()`
- `getAuthId()`
- `getAuthType()`
- `getObjectsType()`
- `getStorageBucket()`
- `getDomainURL()` for IDCS identity-domain logout/session flows
- `getSchema()`
- `getApiVersion()`
- `isUseSocket()`
- `isEnableLogging()`
- `getUploadChunkSize()`
- `getLongPollingInterval()`

## Notes

- The SDK uses Java-style getter methods on `FusabaseOptions`.
- The auto-init path is Android-specific and important for app code generation.
- Named app instances are supported through `FusabaseApp.initializeApp(context, options, name)`.

## Related Docs

- `agent_docs/core.md`
- `agent_docs/auth.md`
