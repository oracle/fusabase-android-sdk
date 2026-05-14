# Core API

Use `com.oracle.mobile.fusabase` for app instances, options, timestamps, and top-level exceptions.

## Primary Types

- `FusabaseApp`
- `FusabaseOptions`
- `Timestamp`
- `FusabaseException`
- `FusabaseNetworkException`
- `FusabaseTooManyRequestsException`

## Common Patterns

### Default app

```java
import com.oracle.mobile.fusabase.FusabaseApp;

FusabaseApp app = FusabaseApp.getInstance();
```

### Manual initialization

```java
FusabaseApp app = FusabaseApp.initializeApp(context, options);
```

### Named app

```java
FusabaseApp analyticsApp = FusabaseApp.initializeApp(context, options, "analytics");
FusabaseApp sameApp = FusabaseApp.getInstance("analytics");
```

### Inspect configuration

```java
FusabaseOptions options = app.getOptions();
String projectId = options.getProjectId();
```

## Notes

- `FusabaseApp.getInstance()` throws if the default app is not initialized.
- Android apps often rely on `FusabaseInitProvider` instead of manual startup code.

## Related Docs

- `agent_docs/configuration.md`
- `agent_docs/auth.md`
- `agent_docs/oracledb.md`
- `agent_docs/storage.md`
