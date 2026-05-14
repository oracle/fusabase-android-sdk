# Storage API

Use `com.oracle.mobile.fusabase.storage` for storage clients, storage references, uploads, downloads, metadata access, listing, and task snapshots.

## Primary Types

- `Storage`
- `StorageReference`
- `StorageMetadata`
- `ListResult`
- `UploadTask`
- `FileDownloadTask`
- `StreamDownloadTask`
- `StorageTask`
- `CancellableTask<StateT>`
- `ControllableTask<StateT>`
- `OnProgressListener<ProgressT>`
- `OnPausedListener<ProgressT>`
- `StorageException`

## Common Patterns

### Get the storage client

```java
import com.oracle.mobile.fusabase.storage.Storage;

Storage storage = Storage.getInstance();
```

### Create references

```java
StorageReference root = storage.getReference();
StorageReference imageRef = storage.getReference("recipes/tea.png");
StorageReference childRef = root.child("recipes/tea.png");
```

### Upload bytes

```java
UploadTask task = imageRef.putBytes(fileBytes);
```

### Upload a file URI

```java
UploadTask task = imageRef.putFile(uri);
```

### Download URL and metadata

```java
imageRef.getDownloadUrl();
imageRef.getMetadata();
```

### List and delete

```java
imageRef.getParent().listAll();
imageRef.delete();
```

### Task lifecycle

```java
UploadTask task = imageRef.putBytes(fileBytes);

task.addOnProgressListener(snapshot ->
    System.out.println(snapshot.getBytesTransferred() + " / " + snapshot.getTotalByteCount())
);
task.addOnPausedListener(snapshot -> System.out.println("paused"));

task.pause();
task.resume();
task.cancel();
```

`UploadTask`, `FileDownloadTask`, and `StreamDownloadTask` extend `ControllableTask`, which adds `pause()`, `resume()`, `isPaused()`, and `addOnPausedListener(...)` on top of `CancellableTask`'s `cancel()`, `isCanceled()`, `isInProgress()`, and `addOnProgressListener(...)`.

## Notes

- Storage operations also use the SDK’s `Task<T>` abstraction and task listeners.
- `StorageReference` is the main handle for paths and objects.
- Upload and download operations expose progress-aware task types such as `UploadTask` and `FileDownloadTask`.

## Related Docs

- `agent_docs/tasks.md`
