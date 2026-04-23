
package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/**
 * Represents the results of a query operation, containing a list of documents
 * that match the query along with metadata about the query execution.
 * Provides access to the documents and any changes since the last query execution.
 */
public class QuerySnapshot extends Snapshot implements Iterable<QueryDocumentSnapshot> {

    /**
     * The metadata associated with this snapshot.
     */
    public final SnapshotMetadata metadata;

    /**
     * The list of document snapshots that match the query.
     */
    private List<QueryDocumentSnapshot> documentSnapshots;

    /**
     * The list of document changes since the last snapshot.
     */
    private List<DocumentChange> documentChanges;

    /**
     * The query that produced this snapshot.
     */
    private final Query query;

    /**
     * Whether this snapshot was retrieved from cache.
     */
    private boolean isFromCache;

    /**
     * Whether this snapshot has pending writes.
     */
    private boolean hasPendingWrites;

    /**
     * Logging tag for this class.
     */
    private static final String TAG = "FusabaseOradb";

    /**
     * Constructs a QuerySnapshot from polling/query data.
     * Creates document changes by comparing with an old snapshot if provided.
     *
     * @param query the query that produced this snapshot
     * @param oldSnap the previous snapshot for change detection, or null
     * @param data the query result data
     */
    protected QuerySnapshot(@NonNull Query query,
                           @Nullable QuerySnapshot oldSnap,
                           @NonNull HashMap<String, Object> data) {
        this.query = query;
        this.metadata = new SnapshotMetadata(false, false);
        this.documentSnapshots = this.getQueryDocSnaps(data);
        this.documentChanges = new ArrayList<>();

        if (oldSnap != null) {
            List<QueryDocumentSnapshot> oldDocs = oldSnap.getQueryDocumentSnapshots();
            Map<String, QueryDocumentSnapshot> oldDocsMap = oldDocs.stream()
                    .collect(Collectors.toMap(DocumentSnapshot::getId, id -> id));
            Map<String, QueryDocumentSnapshot> newDocsMap = this.documentSnapshots.stream()
                    .collect(Collectors.toMap(DocumentSnapshot::getId, id -> id));

            this.documentSnapshots.forEach((doc) -> {
                QueryDocumentSnapshot oldDocSnap = oldDocsMap.get(doc.getId());

                if (oldDocSnap == null ||
                        !doc.getDocument().getLastModified()
                        .equals(oldDocSnap.getDocument().getLastModified())) {
                    this.documentChanges.add(new DocumentChange(doc,
                            doc.getDocIndex().intValue(),
                            oldDocSnap == null ? -1 : oldDocSnap.getDocIndex().intValue(),
                            oldDocSnap == null ? DocumentChange.Type.ADDED
                                    : DocumentChange.Type.MODIFIED));
                }
            });

            oldDocs.forEach((oldDoc) -> {
                DocumentSnapshot newDocSnap = newDocsMap.get(oldDoc.getId());

                if (newDocSnap == null) {
                    this.documentChanges.add(new DocumentChange(oldDoc,
                            -1,
                            oldDoc.getDocIndex().intValue(),
                            DocumentChange.Type.REMOVED));
                }
            });
        } else {
            this.documentSnapshots.forEach((doc) -> {
                this.documentChanges.add(new DocumentChange(doc,
                        doc.getDocIndex().intValue(),
                        -1,
                        DocumentChange.Type.ADDED));
            });
        }
        // Needs to be updated after caching
        this.hasPendingWrites = false;
        this.isFromCache = false;
    }

    /**
     * Constructs a QuerySnapshot from WebSocket real-time notification data.
     * Updates the existing snapshot with the changes from the notification.
     *
     * @param query the query that produced this snapshot
     * @param oldSnap the previous snapshot to update
     * @param changedDataNotification the WebSocket notification data
     * @throws FusabaseOracledbException if the notification data is invalid
     */
    protected QuerySnapshot(@NonNull Query query,
                           @NonNull QuerySnapshot oldSnap,
                           @NonNull JsonObject changedDataNotification) throws FusabaseOracledbException {
        this.query = query;
        this.metadata = new SnapshotMetadata(false, false);
        this.documentChanges = new ArrayList<>();
        this.documentSnapshots = oldSnap.getQueryDocumentSnapshots();
        this.hasPendingWrites = false;
        this.isFromCache = false;

        List<QueryDocumentSnapshot> updatedQueryDocSnap = oldSnap.getQueryDocumentSnapshots();

        HashMap<String, Object> changedData = DataReader.getJsonObjectDataInMap(changedDataNotification.getJsonObject("changedData"));

        JsonArray operationsArray = changedDataNotification.getJsonArray("operations");
        String operation = operationsArray.getString(operationsArray.size()-1);

        HashMap<String, Object> documentData = changedData.containsKey("DOCUMENT") ?
                (HashMap<String, Object>) changedData.get("DOCUMENT") :
                changedData;

        List<String> notificationPath = new ArrayList<>(this.query.path);

        QueryDocumentSnapshot oldDoc = oldSnap.getQueryDocumentSnapshots().stream()
                .filter((q) -> q.getDocument().getRowId().equals(changedDataNotification.getString("rowId")))
                .findFirst()
                .orElse(null);

        if (documentData != null) {
            notificationPath.add((String) documentData.get("OID"));
        } else {
            if (!operation.equals("DELETE")) {
                throw new FusabaseOracledbException("Empty body received in notification" +
                        " where operation is not DELETE. The notification is as follows "
                        + changedDataNotification.toString(), FusabaseOracledbException.Code.INTERNAL);
            }

            if (oldDoc == null) {
                FusabaseLogger.w(TAG, "DELETE notification received for document not in snapshot: " + changedDataNotification.getString("rowId"));
                // Document already removed or not in scope, skip processing
                this.documentSnapshots = oldSnap.getQueryDocumentSnapshots();
                this.documentChanges = new ArrayList<>();
                return;
            }

            notificationPath.add(oldDoc.getDocument().getId());
        }

        // Clean the data of BAAS Fields
        if(documentData != null) {
            documentData.remove("OID");
            documentData.remove("_metadata");
            documentData.remove("parent_oid");
        }

        String path = String.join("/", notificationPath);

        StringBuilder currPath = new StringBuilder();
        Object parent = null;
        Object current = null;

        for(int i = 0; i < notificationPath.size(); i++)
        {
            currPath.append(notificationPath.get(i));
            if(i%2 == 0)
            {
                current = new CollectionReference(currPath.toString(), (DocumentReference) parent, this.query.oradb);
            } else {
                current = new DocumentReference(currPath.toString(), (CollectionReference) parent, this.query.oradb, true);
            }
            parent  = current;
            currPath.append("/");
        }

        DocumentChange.Type docChangeType = operation.equals("INSERT") ? DocumentChange.Type.ADDED :
                operation.equals("UPDATE") ? DocumentChange.Type.MODIFIED : DocumentChange.Type.REMOVED;

        String version = changedData.containsKey("VERSION") ? (String) changedData.get("VERSION")
                : changedData.containsKey("_metadata") ?  (String) ((HashMap<String, Object>) changedData.get("_metadata")).get("etag") : "";
        String asof = changedData.containsKey("_metadata") ? (String) ((HashMap<String, Object>) changedData.get("_metadata")).getOrDefault("asof", "") : "";

        // For relational views, compare asof values to avoid processing stale notifications
        if (docChangeType == DocumentChange.Type.MODIFIED && oldDoc != null && !asof.isEmpty() && !oldDoc.getDocument().getAsof().isEmpty()) {
            if (asof.compareTo(oldDoc.getDocument().getAsof()) <= 0) {
                // Notification is stale or same as current, skip processing
                this.documentSnapshots = oldSnap.getQueryDocumentSnapshots();
                this.documentChanges = new ArrayList<>();
                this.hasPendingWrites = false;
                this.isFromCache = false;
                return;
            }
        }

        Document doc = new Document(documentData == null ? null : documentData,
                    (String) changedData.getOrDefault("OID", ""),
                    changedData.getOrDefault("CREATED", "").toString(),
                    changedData.getOrDefault("LAST_MODIFIED", "").toString(),
                    version,
                    asof,
                    oldDoc == null ? oldSnap.documentSnapshots.size() : oldDoc.getDocIndex(),
                    notificationPath,
                    changedDataNotification.getString("rowId"));

        QueryDocumentSnapshot newDocSnap = new QueryDocumentSnapshot(this.query.oradb,
                    doc,
                    (DocumentReference) current,
                    path,
                    oldSnap.isFromCache,
                    oldSnap.hasPendingWrites);

        try {
            switch (docChangeType) {
                case ADDED:
                    updatedQueryDocSnap.add(newDocSnap);
                    this.documentChanges.add(new DocumentChange(newDocSnap, oldSnap.getQueryDocumentSnapshots().size(), -1, docChangeType));
                    break;
                case MODIFIED: {
                    if(oldDoc == null) {
                        FusabaseLogger.w(TAG, "UPDATE notification received for document not in snapshot: " + changedDataNotification.getString("rowId"));
                        // Document already updated or not in scope, skip processing
                        break;
                    }
                    updatedQueryDocSnap.replaceAll(queryDocSnap ->
                            (queryDocSnap.getDocument().getRowId().equals(changedDataNotification.get("rowId"))) ? newDocSnap : queryDocSnap);
                    this.documentChanges.add(new DocumentChange(newDocSnap, oldDoc.getDocIndex().intValue(), oldDoc.getDocIndex().intValue(), docChangeType));
                    break;
                }
                case REMOVED: {
                    if(oldDoc == null) {
                        FusabaseLogger.w(TAG, "DELETE notification received for document not in snapshot: " + changedDataNotification.getString("rowId"));
                        // Document already removed or not in scope, skip
                        break;
                    }
                    this.documentChanges.add(new DocumentChange(newDocSnap, -1, oldDoc.getDocIndex().intValue(), docChangeType));
                    updatedQueryDocSnap.remove(oldDoc);
                    break;
                }
            }
        } catch (Exception e) {
            FusabaseLogger.e(TAG, e.getMessage());
            throw new FusabaseOracledbException(e.getMessage(), FusabaseOracledbException.Code.INTERNAL);
        }

        this.documentSnapshots = updatedQueryDocSnap;
        this.hasPendingWrites = false;
        this.isFromCache = false;
    }

    /**
     * Checks if this QuerySnapshot is equal to another object.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof QuerySnapshot) {
            QuerySnapshot snapshot = (QuerySnapshot) obj;
            return this.metadata.equals(snapshot.metadata) &&
                    this.documentSnapshots.equals(snapshot.documentSnapshots) &&
                    this.documentChanges.equals(snapshot.documentChanges) &&
                    this.query.equals(snapshot.query);
        }
        return false;
    }

    /**
     * Returns a hash code value for this QuerySnapshot.
     * @return int hash code value.
     */
    public int hashCode() {
        return java.util.Objects.hash(metadata, documentSnapshots, documentChanges, query);
    }

    /**
     * Gets the list of document changes since the last snapshot.
     *
     * @return the list of document changes
     */
    @NonNull
    public List<DocumentChange> getDocumentChanges() {
        return this.documentChanges;
    }

    /**
     * Gets the list of all documents in this snapshot.
     *
     * @return the list of document snapshots
     */
    @NonNull
    public List<DocumentSnapshot> getDocuments() {
        return new ArrayList<>(this.documentSnapshots);
    }

    /**
     * Gets the metadata associated with this snapshot.
     *
     * @return the snapshot metadata
     */
    @NonNull
    public SnapshotMetadata getMetadata() {
        return this.metadata;
    }

    /**
     * Gets the query that produced this snapshot.
     *
     * @return the query object
     */
    @NonNull
    public Query getQuery() {
        return this.query;
    }

    /**
     * Checks if this snapshot contains no documents.
     *
     * @return true if the snapshot is empty, false otherwise
     */
    public boolean isEmpty() {
        return this.documentSnapshots.isEmpty();
    }

    /**
     * Returns an iterator over the query document snapshots.
     *
     * @return an iterator for the document snapshots
     */
    @NonNull
    @Override
    public Iterator<QueryDocumentSnapshot> iterator() {
        return documentSnapshots.iterator();
    }

    /**
     * Gets the number of documents in this snapshot.
     *
     * @return the number of documents
     */
    public int size() {
        return this.documentSnapshots.size();
    }

    /**
     * Gets the list of query document snapshots.
     *
     * @return the list of query document snapshots
     */
    @NonNull
    protected List<QueryDocumentSnapshot> getQueryDocumentSnapshots() {
        return this.documentSnapshots;
    }

    /**
     * Converts query payload data into a list of QueryDocumentSnapshot objects.
     *
     * @param payload the query result payload data
     * @return the list of query document snapshots
     */
    @NonNull
    private List<QueryDocumentSnapshot> getQueryDocSnaps(@NonNull HashMap<String, Object> payload) {
        List<QueryDocumentSnapshot> queryDocSnaps = new ArrayList<>();

        payload.forEach((key, value) -> {
            if (Objects.equals(key, "ret"))
            {
                for(HashMap<String, Object> data : (ArrayList<HashMap<String, Object>>)value) {
                    String rowId = data.getOrDefault("rid", "").toString();
                    HashMap<String, Object> dataValue = (HashMap<String, Object>) data.get("osons");
                    List<String> currPathArray = new ArrayList<>(this.query.path);

                    // Document
                    HashMap<String, Object> document;
                    String version = "";
                    Object indexObj = dataValue.getOrDefault("INDEX", -1);
                    Long index = indexObj instanceof Number ? ((Number) indexObj).longValue() : -1L;

                    String asof = "";
                    if(this.query.isDualityView())
                    {
                        document = dataValue;
                        version = ((HashMap<String, Object>) dataValue.get("_metadata"))
                                .getOrDefault("etag", "").toString();
                        asof = ((HashMap<String, Object>) dataValue.get("_metadata"))
                                .getOrDefault("asof", "").toString();
                    }
                    else {
                        // Document Model
                        // Relational Model

                        document = dataValue.containsKey("DOCUMENT") ?
                                (HashMap<String, Object>) dataValue.get("DOCUMENT") :
                                dataValue;
                        if (dataValue.containsKey("_metadata")) {
                            HashMap<String, Object> metadata = (HashMap<String, Object>) dataValue.get("_metadata");
                            version = metadata.get("etag").toString();
                            asof = metadata.getOrDefault("asof", "").toString();
                        } else {
                            version = String.valueOf(dataValue.getOrDefault("VERSION", ""));
                            asof = "";
                        }
                    }

                    // OID
                    String OID = "";
                    if(dataValue.containsKey("OID"))
                        OID = (String) dataValue.get("OID");
                    else if(dataValue.containsKey("oid"))
                        OID = (String) dataValue.get("oid");

                    if(this.query.joins.isEmpty())
                        currPathArray.add(OID);

                    Object createdObject = dataValue.getOrDefault("CREATED", "");
                    Object lastModifiedObject = dataValue.getOrDefault("LAST_MODIFIED", "");
                    String created = createdObject instanceof Timestamp ? ((Timestamp)createdObject).getString() : "";
                    String lastModified = lastModifiedObject instanceof Timestamp ? ((Timestamp)lastModifiedObject).getString() : "";

                    // These fields will be removed if the response is for relational
                    document.remove("_metadata");
                    document.remove("parent_oid");
                    document.remove("OID");

                    Document doc = new Document(document,
                            OID,
                            created,
                            lastModified,
                            version,
                            asof,
                            index,
                            currPathArray,
                            rowId);

                    Object current = null;
                    Object parent = null;
                    String path = "";

                    if(this.query.joins.isEmpty()) {
                        path = String.join("/", currPathArray);

                        StringBuilder currPath = new StringBuilder();

                        for (int i = 0; i < currPathArray.size(); i++) {
                            currPath.append(currPathArray.get(i));
                            if (i % 2 == 0) {
                                current = new CollectionReference(currPath.toString(), (DocumentReference) parent, this.query.oradb);
                            } else {
                                current = new DocumentReference(currPath.toString(), (CollectionReference) parent, this.query.oradb, true);
                            }
                            parent = current;
                            currPath.append("/");
                        }
                    } else {
                        current = new DocumentReference("", null, this.query.oradb, true);
                    }

                    queryDocSnaps.add(new QueryDocumentSnapshot(this.query.oradb,
                            doc,
                            (DocumentReference) current,
                            path,
                            this.isFromCache,
                            this.hasPendingWrites
                    ));
                }
            }
            else {
                FusabaseLogger.d("QuerySnapshot", "Invalid Data at QuerySnapshot");
                throw new RuntimeException("Invalid Data at Query Snapshot");
            }
        });

        return queryDocSnaps;
    }
}
