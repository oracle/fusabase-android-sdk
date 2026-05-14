# Auth API

Use `com.oracle.mobile.fusabase.auth` for sign-in, account creation, current-user access, provider flows, password flows, and token/auth listeners.

## Primary Types

- `FusabaseAuth`
- `FusabaseUser`
- `AuthResult`
- `AuthCredential`
- `GetTokenResult`
- `UserProfileChangeRequest`
- Provider types such as `EmailAuthProvider`, `GoogleAuthProvider`, `FacebookAuthProvider`, `GithubAuthProvider`, `IDCSAuthProvider`, `OAuthProvider`, and `SAMLAuthProvider`

## Common Patterns

### Get the auth instance

```java
import com.oracle.mobile.fusabase.auth.FusabaseAuth;

FusabaseAuth auth = FusabaseAuth.getInstance();
```

Or bind auth to a specific app:

```java
FusabaseAuth auth = FusabaseAuth.getInstance(app);
```

### Create and sign in users

```java
auth.createUserWithEmailAndPassword(email, password);
auth.signInWithEmailAndPassword(email, password);
```

These methods return `Task<AuthResult>`.

### Sign in with credentials

```java
AuthCredential credential = EmailAuthProvider.getCredential(email, password);
auth.signInWithCredential(credential);
```

### Auth-state listeners

```java
auth.addAuthStateListener(fusabaseAuth -> {
    if (fusabaseAuth.getCurrentUser() != null) {
        System.out.println(fusabaseAuth.getCurrentUser().getEmail());
    }
});
```

### Token listeners

```java
auth.addIdTokenListener(fusabaseAuth -> {
    if (fusabaseAuth.getCurrentUser() != null) {
        fusabaseAuth.getCurrentUser().getIdToken(false);
    }
});
```

### Password reset

```java
auth.sendPasswordResetEmail(email);
```

### IDCS sign-in setup

IDCS-backed apps are configured at app init via `FusabaseOptions`. Set `authType` to `"idcs"` and supply an `IDCSOptions` instance.

```java
import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.models.IDCSOptions;

IDCSOptions idcs = new IDCSOptions.Builder()
    .setClientId("client-id")
    .setClientSecret("client-secret")
    .setDomainURL("https://example.identity.oracle.com")
    .build();

FusabaseOptions options = new FusabaseOptions.Builder()
    .setOrdsHost("https://example/ords/schema/")
    .setProjectId("project-id")
    .setAppId("app-id")
    .setAuthType("idcs")
    .setAuthId("auth-id")
    .setIDCSOptions(idcs)
    .build();

FusabaseApp app = FusabaseApp.initializeApp(context, options);
FusabaseAuth auth = FusabaseAuth.getInstance(app);
```

Use `setAuthType("onprem")` to point at an on-prem identity-manager backend instead. See `agent_docs/configuration.md` for the full options surface.

## Notes

- The module uses the SDK’s own `Task<T>` abstraction, not Google Play Services `Task<T>`.
- Provider classes expose `PROVIDER_ID` constants and credential helpers.
- Android provider flows can involve activities and redirect handling, so preserve existing Android lifecycle patterns in consuming apps.

## Related Docs

- `agent_docs/tasks.md`
