# OracleDB API

Use `com.oracle.mobile.fusabase.oracledb` for document-style data access, queries, snapshot listeners, transactions, write batches, aggregates, vector search, and duality views.

## Primary Types

- `FusabaseOracledb`
- `CollectionReference`
- `DocumentReference`
- `Query`
- `QuerySnapshot`
- `DocumentSnapshot`
- `WriteBatch`
- `Transaction`
- `Filter`
- `FieldPath`
- `FieldValue`
- `AggregateQuery`
- `AggregateQuerySnapshot`
- `AggregateField`
- `FindNearestQuery`
- `FindNearestOptions`
- `VectorMetric`
- `DenseVector`
- `SparseVector`
- `EventListener<T>`
- `ListenerRegistration`
- `DualityViewColReference`
- `DualityViewDocReference`

## Common Patterns

### Get the database instance

```java
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;

FusabaseOracledb db = FusabaseOracledb.getInstance();
```

### Collections and documents

```java
CollectionReference recipes = db.collection("recipes");
DocumentReference recipe = recipes.document("tea");
recipe.get();
```

### Add and update documents

```java
java.util.Map<String, Object> data = new java.util.HashMap<>();
data.put("title", "Tea");
data.put("category", "Drinks");

recipes.addDocument(data);
recipe.update("category", "Hot Drinks");
```

### Queries

```java
Query query = recipes
    .whereEqualTo("category", "Drinks")
    .orderBy("title")
    .limit(20);

query.get();
```

### Snapshot listeners

```java
ListenerRegistration registration = query.addSnapshotListener((snap, error) -> {
    if (snap != null) {
        System.out.println(snap.getDocuments().size());
    }
});

// Detach when finished.
registration.remove();
```

The lambda is an `EventListener<QuerySnapshot>`. Overloads accept an `Activity`, `Executor`, `MetadataChanges`, or full `SnapshotListenOptions`.

### Vector search

```java
import java.util.Arrays;
import com.oracle.mobile.fusabase.oracledb.FindNearestOptions;
import com.oracle.mobile.fusabase.oracledb.FindNearestQuery;
import com.oracle.mobile.fusabase.oracledb.VectorMetric;

FindNearestQuery query = FindNearestQuery.dense(Arrays.asList(0.22, 0.93, -0.1));
FindNearestOptions options = FindNearestOptions.builder()
    .metric(VectorMetric.COSINE)
    .topK(10)
    .build();

Query nearest = recipes.findNearest("EMB", query, options);
nearest.get();
```

For sparse embeddings, build a `SparseVector` and pass it to `FindNearestQuery.sparse(...)`.

### Transactions and batches

```java
db.runTransaction(transaction -> {
    DocumentSnapshot snapshot = transaction.get(recipe);
    transaction.update(recipe, "views", 1L);
    return snapshot;
});

WriteBatch batch = db.batch();
batch.update(recipe, "published", true);
batch.commit();
```

## Other Public Areas

- aggregate queries via `AggregateField`, `AggregateQuery`, and `AggregateQuerySnapshot`
- vector search via `FindNearestQuery`, `FindNearestOptions`, and `VectorMetric`
- duality-view helpers
- bulk updates
- `FieldValue` sentinels such as `arrayUnion`, `arrayRemove`, `increment`, `serverTimestamp`, and `delete`

## Notes

- Most read and write operations return the SDK’s `Task<T>` type.
- Query composition is chain-based and returns updated `Query` instances.
- Listener APIs support overloads for activities, executors, metadata changes, and snapshot options.

## Related Docs

- `agent_docs/tasks.md`
- `agent_docs/storage.md`
