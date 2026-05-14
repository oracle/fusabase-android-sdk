# Task API

The Android SDK uses its own task abstraction in `com.oracle.mobile.fusabase.task`.

## Primary Types

- `Task<T>`
- `TaskCompletionSource<T>`
- `OnSuccessListener<T>`
- `OnFailureListener`
- `OnCompleteListener<T>`
- `OnCanceledListener`
- `Tasks`

## Why This Matters

Auth, database, and storage APIs commonly return `Task<T>` rather than blocking values. Agents generating Android code should preserve that async style instead of inventing synchronous wrappers.

## Common Pattern

```java
auth.signInWithEmailAndPassword(email, password)
    .addOnSuccessListener(result -> {
        System.out.println(result.getUser().getEmail());
    })
    .addOnFailureListener(error -> {
        error.printStackTrace();
    });
```

## Storage Task Pattern

```java
imageRef.putBytes(fileBytes)
    .addOnProgressListener(snapshot -> {
        System.out.println(snapshot.getBytesTransferred());
    })
    .addOnSuccessListener(snapshot -> {
        System.out.println("Upload complete");
    });
```

## Notes

- This task model is part of the public SDK surface.
- Storage task types add progress and pause/resume controls on top of the base task pattern.

## Related Docs

- `agent_docs/auth.md`
- `agent_docs/oracledb.md`
- `agent_docs/storage.md`
