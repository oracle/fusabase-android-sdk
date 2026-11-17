// Copyright (c) 2015, 2025, Oracle and/or its affiliates.

//-----------------------------------------------------------------------------
//
// This software is dual-licensed to you under the Universal Permissive License
// (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl and Apache License
// 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
// either license.
//
// If you elect to accept the software under the Apache License, Version 2.0,
// the following applies:
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//-----------------------------------------------------------------------------
package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.offline.OfflineStoreManager;
import com.oracle.mobile.fusabase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
 *
 * <p>Example:</p>
 * <pre>{@code
 * query.get().addOnSuccessListener(snapshot -> {
 *     for (DocumentSnapshot document : snapshot.getDocuments()) {
 *         String id = document.getId();
 *     }
 * });
 * }</pre>
 */
public class QuerySnapshot extends Snapshot implements Iterable<QueryDocumentSnapshot> {

    /**
     * The metadata associated with this snapshot.
     *
     * <p>Example:</p>
     * <pre>{@code
     * query.get().addOnSuccessListener(snapshot -> {
     *     for (DocumentSnapshot document : snapshot.getDocuments()) {
     *         String id = document.getId();
     *     }
     * });
     * }</pre>
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

    protected QuerySnapshot(@NonNull Query query,
                            @NonNull List<QueryDocumentSnapshot> documentSnapshots,
                            boolean isFromCache,
                            boolean hasPendingWrites) {
        this.query = query;
        this.metadata = new SnapshotMetadata(hasPendingWrites, isFromCache);
        this.documentSnapshots = new ArrayList<>(documentSnapshots);
        this.documentChanges = new ArrayList<>();
        for (int i = 0; i < this.documentSnapshots.size(); i++) {
            this.documentChanges.add(new DocumentChange(this.documentSnapshots.get(i), i, -1,
                    DocumentChange.Type.ADDED));
        }
        this.hasPendingWrites = hasPendingWrites;
        this.isFromCache = isFromCache;
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
        this.documentSnapshots = new ArrayList<>(oldSnap.getQueryDocumentSnapshots());
        this.hasPendingWrites = false;
        this.isFromCache = false;

        List<QueryDocumentSnapshot> updatedQueryDocSnap = new ArrayList<>(oldSnap.getQueryDocumentSnapshots());

        boolean usesRootMetadata = RealtimeNotification.hasRootMetadata(changedDataNotification);
        HashMap<String, Object> changedData = DataReader.getJsonObjectDataInMap(
                RealtimeNotification.changedData(changedDataNotification));

        JsonArray operationsArray = changedDataNotification.getJsonArray("operations");
        String operation = operationsArray.getString(operationsArray.size()-1);

        HashMap<String, Object> documentData = usesRootMetadata ? new HashMap<>(changedData) :
                changedData.containsKey("DOCUMENT") ?
                        new HashMap<>((HashMap<String, Object>) changedData.get("DOCUMENT")) :
                        new HashMap<>(changedData);

        List<String> notificationPath = new ArrayList<>(this.query.path);

        QueryDocumentSnapshot oldDoc = oldSnap.getQueryDocumentSnapshots().stream()
                .filter((q) -> q.getDocument().getRowId().equals(changedDataNotification.getString("rowId")))
                .findFirst()
                .orElse(null);

        // Capture the public document ID before the server envelope is removed from the data map.
        // Realtime payloads may place OID directly in changedData or inside DOCUMENT.
        String objectId = usesRootMetadata ? RealtimeNotification.oid(changedDataNotification) :
                getObjectId(documentData, changedData);
        if (objectId.isEmpty() && oldDoc != null) {
            objectId = oldDoc.getDocument().getId();
        }
        if (objectId.isEmpty() && !operation.equals("DELETE")) {
            throw new FusabaseOracledbException("Realtime notification is missing document OID",
                    FusabaseOracledbException.Code.INTERNAL);
        }

        // Some transports do not include the row identifier on every notification. Fall back to
        // the stable document identifier so stale-event filtering and membership reconciliation
        // still operate on the existing snapshot entry.
        if (oldDoc == null && !objectId.isEmpty()) {
            String notifiedObjectId = objectId;
            oldDoc = oldSnap.getQueryDocumentSnapshots().stream()
                    .filter((q) -> q.getId().equals(notifiedObjectId))
                    .findFirst()
                    .orElse(null);
        }

        if (documentData != null) {
            notificationPath.add(objectId);
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

        String version = RealtimeNotification.version(changedDataNotification);
        String asof = RealtimeNotification.asOf(changedDataNotification);

        // Clean the data of BAAS Fields
        if (!usesRootMetadata && documentData != null) {
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

        if (oldDoc != null && RealtimeVersion.isIncomingOlder(oldDoc.getDocument().getVersion(), version)) {
            // Preserve the newer cached document and its query membership. Equal or unordered
            // versions remain observable, exactly as in the JS realtime implementation.
            this.documentSnapshots = oldSnap.getQueryDocumentSnapshots();
            this.documentChanges = new ArrayList<>();
            this.hasPendingWrites = false;
            this.isFromCache = false;
            return;
        }

        if (docChangeType == DocumentChange.Type.REMOVED) {
            OfflineStoreManager.saveNoDocument(query.oradb, path);
        } else {
            OfflineStoreManager.saveRemoteDocument(query.oradb, path,
                    documentData == null ? new HashMap<>() : documentData, version);
        }

        Document doc = new Document(documentData == null ? null : documentData,
                    objectId,
                    RealtimeNotification.created(changedDataNotification),
                    RealtimeNotification.lastModified(changedDataNotification),
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
            if (docChangeType == DocumentChange.Type.REMOVED ||
                    !OfflineSnapshotFactory.matchesQueryDocument(query, newDocSnap)) {
                if (oldDoc != null) {
                    updatedQueryDocSnap.remove(oldDoc);
                }
            } else if (oldDoc == null) {
                updatedQueryDocSnap.add(newDocSnap);
            } else {
                int oldIndex = updatedQueryDocSnap.indexOf(oldDoc);
                updatedQueryDocSnap.set(oldIndex, newDocSnap);
            }
            if (OfflineStoreManager.isEnabled(query.oradb)) {
                String pathPrefix = query.isColGroup ? "" : String.join("/", query.path) + "/";
                updatedQueryDocSnap = OfflineSnapshotFactory.materializeCachedQuery(query,
                        OfflineStoreManager.getCachedDocumentsWithPrefix(query.oradb, pathPrefix), false);
            } else {
                updatedQueryDocSnap = OfflineSnapshotFactory.applyQueryConstraints(query, updatedQueryDocSnap);
            }
            this.documentChanges = getDocumentChanges(oldSnap.getQueryDocumentSnapshots(), updatedQueryDocSnap);
        } catch (Exception e) {
            FusabaseLogger.e(TAG, e.getMessage());
            throw new FusabaseOracledbException(e.getMessage(), FusabaseOracledbException.Code.INTERNAL);
        }

        this.documentSnapshots = updatedQueryDocSnap;
        this.hasPendingWrites = false;
        this.isFromCache = false;
    }

    private static List<DocumentChange> getDocumentChanges(
            @NonNull List<QueryDocumentSnapshot> oldDocuments,
            @NonNull List<QueryDocumentSnapshot> newDocuments) {
        Map<String, Integer> oldIndexes = new LinkedHashMap<>();
        Map<String, QueryDocumentSnapshot> oldByRowId = new LinkedHashMap<>();
        for (int index = 0; index < oldDocuments.size(); index++) {
            QueryDocumentSnapshot document = oldDocuments.get(index);
            oldIndexes.put(document.getDocument().getRowId(), index);
            oldByRowId.put(document.getDocument().getRowId(), document);
        }
        Map<String, Integer> newIndexes = new LinkedHashMap<>();
        Map<String, QueryDocumentSnapshot> newByRowId = new LinkedHashMap<>();
        for (int index = 0; index < newDocuments.size(); index++) {
            QueryDocumentSnapshot document = newDocuments.get(index);
            newIndexes.put(document.getDocument().getRowId(), index);
            newByRowId.put(document.getDocument().getRowId(), document);
        }

        List<DocumentChange> changes = new ArrayList<>();
        for (Map.Entry<String, QueryDocumentSnapshot> oldEntry : oldByRowId.entrySet()) {
            if (!newByRowId.containsKey(oldEntry.getKey())) {
                changes.add(new DocumentChange(oldEntry.getValue(), -1,
                        oldIndexes.get(oldEntry.getKey()), DocumentChange.Type.REMOVED));
            }
        }
        for (Map.Entry<String, QueryDocumentSnapshot> newEntry : newByRowId.entrySet()) {
            QueryDocumentSnapshot oldDocument = oldByRowId.get(newEntry.getKey());
            int newIndex = newIndexes.get(newEntry.getKey());
            if (oldDocument == null) {
                changes.add(new DocumentChange(newEntry.getValue(), newIndex, -1,
                        DocumentChange.Type.ADDED));
            } else {
                int oldIndex = oldIndexes.get(newEntry.getKey());
                if (!sameDocumentContent(oldDocument.getDocument(), newEntry.getValue().getDocument()) ||
                        oldIndex != newIndex) {
                    changes.add(new DocumentChange(newEntry.getValue(), newIndex, oldIndex,
                            DocumentChange.Type.MODIFIED));
                }
            }
        }
        return changes;
    }

    private static boolean sameDocumentContent(@NonNull Document left, @NonNull Document right) {
        return Objects.equals(left.getData(), right.getData()) &&
                Objects.equals(left.getId(), right.getId()) &&
                Objects.equals(left.getCreated(), right.getCreated()) &&
                Objects.equals(left.getLastModified(), right.getLastModified()) &&
                Objects.equals(left.getVersion(), right.getVersion()) &&
                Objects.equals(left.getAsof(), right.getAsof()) &&
                Objects.equals(left.getRowId(), right.getRowId());
    }

    boolean hasDocumentChanges() {
        return !documentChanges.isEmpty();
    }

    /**
     * Checks if this QuerySnapshot is equal to another object.
     *
     *
     * <p>Example: Given compatible values named as shown.</p>
     * <pre>{@code
     * boolean same = first.equals(second);
     * }</pre>
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
     *
     * <p>Example: Given compatible values named as shown.</p>
     * <pre>{@code
     * int hash = value.hashCode();
     * }</pre>
     *
     * @return int hash code value.
     */
    public int hashCode() {
        return java.util.Objects.hash(metadata, documentSnapshots, documentChanges, query);
    }

    /**
     * Gets the list of document changes since the last snapshot.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * query.get().addOnSuccessListener(snapshot -> {
     *     for (DocumentSnapshot document : snapshot.getDocuments()) {
     *         String id = document.getId();
     *     }
     * });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * query.get().addOnSuccessListener(snapshot -> {
     *     for (DocumentSnapshot document : snapshot.getDocuments()) {
     *         String id = document.getId();
     *     }
     * });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * query.get().addOnSuccessListener(snapshot -> {
     *     for (DocumentSnapshot document : snapshot.getDocuments()) {
     *         String id = document.getId();
     *     }
     * });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * query.get().addOnSuccessListener(snapshot -> {
     *     for (DocumentSnapshot document : snapshot.getDocuments()) {
     *         String id = document.getId();
     *     }
     * });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * query.get().addOnSuccessListener(snapshot -> {
     *     for (DocumentSnapshot document : snapshot.getDocuments()) {
     *         String id = document.getId();
     *     }
     * });
     * }</pre>
     *
     * @return true if the snapshot is empty, false otherwise
     */
    public boolean isEmpty() {
        return this.documentSnapshots.isEmpty();
    }

    /**
     * Returns an iterator over the query document snapshots.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * query.get().addOnSuccessListener(snapshot -> {
     *     for (DocumentSnapshot document : snapshot.getDocuments()) {
     *         String id = document.getId();
     *     }
     * });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * query.get().addOnSuccessListener(snapshot -> {
     *     for (DocumentSnapshot document : snapshot.getDocuments()) {
     *         String id = document.getId();
     *     }
     * });
     * }</pre>
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

                    // Capture OID before removing server-managed fields from the user-facing data.
                    String OID = getObjectId(document, dataValue);

                    if(this.query.joins.isEmpty())
                        currPathArray.add(OID);

                    Object createdObject = dataValue.getOrDefault("CREATED", "");
                    Object lastModifiedObject = dataValue.getOrDefault("LAST_MODIFIED", "");
                    String created = createdObject instanceof Timestamp ? ((Timestamp)createdObject).getString() : "";
                    String lastModified = lastModifiedObject instanceof Timestamp ? ((Timestamp)lastModifiedObject).getString() : "";

                    // These fields belong to the SDK/server envelope and must not be exposed
                    // as application data. Copy the map before removing them so parsing a
                    // snapshot does not mutate the response object owned by the caller.
                    document = new HashMap<>(document);
                    document.remove("_metadata");
                    document.remove("parent_oid");
                    document.remove("OID");
                    document.remove("INDEX");

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

    /**
     * Reads the stable document identifier from either a document body or its response envelope.
     * The maps are intentionally kept separate because the body is cleaned before constructing
     * the caller-visible Document.
     */
    @NonNull
    private static String getObjectId(@Nullable Map<String, Object> documentData,
                                      @Nullable Map<String, Object> responseData) {
        String objectId = objectIdValue(documentData, "OID");
        if (objectId.isEmpty()) {
            objectId = objectIdValue(documentData, "oid");
        }
        if (objectId.isEmpty()) {
            objectId = objectIdValue(responseData, "OID");
        }
        if (objectId.isEmpty()) {
            objectId = objectIdValue(responseData, "oid");
        }
        return objectId;
    }

    @NonNull
    private static String objectIdValue(@Nullable Map<String, Object> data,
                                        @NonNull String key) {
        if (data == null || data.get(key) == null) {
            return "";
        }
        return String.valueOf(data.get(key));
    }
}
