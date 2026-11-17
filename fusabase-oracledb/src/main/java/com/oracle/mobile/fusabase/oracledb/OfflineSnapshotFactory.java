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

import com.oracle.mobile.fusabase.oracledb.offline.OfflineCachedDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

final class OfflineSnapshotFactory {
    private OfflineSnapshotFactory() { }

    @NonNull
    static DocumentSnapshot fromCachedDocument(@NonNull DocumentReference reference,
                                               @NonNull OfflineCachedDocument cachedDocument) {
        boolean hasPendingWrites = cachedDocument.hasLocalMutations();
        if (cachedDocument.getState() == OfflineCachedDocument.State.NO_DOCUMENT) {
            return new DocumentSnapshot(reference.getOradb(), null, reference,
                    reference.getPath(), true, hasPendingWrites);
        }

        HashMap<String, Object> data = toHashMap(cachedDocument.getData());
        List<String> pathSegments = new ArrayList<>(reference.getPathSegments());
        String documentId = pathSegments.isEmpty() ? reference.getPath() :
                pathSegments.get(pathSegments.size() - 1);
        String updateTime = toTimestampString(cachedDocument.getUpdateTimeMillis());
        String readTime = toTimestampString(cachedDocument.getReadTimeMillis());
        String version = cachedDocument.getVersion().isEmpty() ?
                TransactionOperation.DEFAULT_VERSION : cachedDocument.getVersion();

        Document document = new Document(data, documentId, readTime, updateTime, version,
                "", null, pathSegments, "");
        return new DocumentSnapshot(reference.getOradb(), document, reference,
                reference.getPath(), true, hasPendingWrites);
    }

    @NonNull
    static QuerySnapshot fromCachedQuery(@NonNull Query query,
                                         @NonNull List<OfflineCachedDocument> cachedDocuments) {
        List<QueryDocumentSnapshot> snapshots = materializeCachedQuery(query, cachedDocuments, true);
        return new QuerySnapshot(query, snapshots, true, hasPendingWrites(snapshots));
    }

    /** Materializes cached documents for either a cache read or a remote event reconciliation. */
    @NonNull
    static List<QueryDocumentSnapshot> materializeCachedQuery(@NonNull Query query,
                                                               @NonNull List<OfflineCachedDocument> cachedDocuments,
                                                               boolean isFromCache) {
        List<QueryDocumentSnapshot> snapshots = new ArrayList<>();
        for (OfflineCachedDocument cachedDocument : cachedDocuments) {
            if (cachedDocument.getState() != OfflineCachedDocument.State.FOUND ||
                    !isDirectQueryDocument(query, cachedDocument.getPath()) ||
                    !matchesFilters(query, cachedDocument)) {
                continue;
            }
            snapshots.add(toQueryDocumentSnapshot(query, cachedDocument, snapshots.size(), isFromCache));
        }
        return applyQueryConstraints(query, snapshots);
    }

    /** Applies the locally-supported query constraints and assigns final document indices. */
    @NonNull
    static List<QueryDocumentSnapshot> applyQueryConstraints(
            @NonNull Query query, @NonNull List<QueryDocumentSnapshot> snapshots) {
        List<QueryDocumentSnapshot> constrained = new ArrayList<>();
        for (QueryDocumentSnapshot snapshot : snapshots) {
            if (matchesQueryDocument(query, snapshot)) {
                constrained.add(snapshot);
            }
        }
        if (!query.explicitOrder.isEmpty()) {
            Collections.sort(constrained, getComparator(query));
        }
        if (query.limit > 0 && constrained.size() > query.limit) {
            constrained = new ArrayList<>(constrained.subList(0, (int) query.limit));
        }
        return reindex(query, constrained);
    }

    static boolean matchesQueryDocument(@NonNull Query query,
                                        @NonNull QueryDocumentSnapshot snapshot) {
        for (Filter filter : query.conditions) {
            if (!matchesFilter(filter, snapshot.getDocument().getData(), snapshot.getId())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDirectQueryDocument(@NonNull Query query, @NonNull String documentPath) {
        List<String> documentSegments = splitPath(documentPath);
        if (query.isColGroup) {
            return documentSegments.size() >= 2 &&
                    query.collectionId.equals(documentSegments.get(documentSegments.size() - 2));
        }
        return documentSegments.size() == query.path.size() + 1 &&
                documentPath.startsWith(String.join("/", query.path) + "/");
    }

    private static boolean matchesFilters(@NonNull Query query,
                                          @NonNull OfflineCachedDocument cachedDocument) {
        for (Filter filter : query.conditions) {
            if (!matchesFilter(filter, cachedDocument.getData(), getDocumentId(cachedDocument))) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean matchesFilter(@NonNull Filter filter,
                                         @Nullable Map<String, Object> documentData,
                                         @NonNull String documentId) {
        Object actual = getFieldValue(documentData, documentId, filter.getField());
        Object expected = filter.getValue();
        switch (filter.getOperator()) {
            case IS_NULL:
                return actual == null;
            case EQUAL:
                return Objects.equals(actual, expected);
            case NOT_EQUAL:
                return !Objects.equals(actual, expected);
            case LESS_THAN:
                return compareValues(actual, expected) < 0;
            case LESS_THAN_OR_EQUAL:
                return compareValues(actual, expected) <= 0;
            case GREATER_THAN:
                return compareValues(actual, expected) > 0;
            case GREATER_THAN_OR_EQUAL:
                return compareValues(actual, expected) >= 0;
            case IN:
                return expected instanceof List && ((List<?>) expected).contains(actual);
            case NOT_IN:
                return expected instanceof List && !((List<?>) expected).contains(actual);
            case ARRAY_CONTAINS:
                return actual instanceof List && ((List<?>) actual).contains(expected);
            case ARRAY_CONTAINS_ANY:
                if (!(actual instanceof List) || !(expected instanceof List)) {
                    return false;
                }
                for (Object value : (List<?>) expected) {
                    if (((List<?>) actual).contains(value)) {
                        return true;
                    }
                }
                return false;
            case LIKE:
                return actual != null && likeMatches(String.valueOf(actual), String.valueOf(expected));
            default:
                throw new IllegalArgumentException("Unsupported local cache filter: " +
                        filter.getOperator());
        }
    }

    @NonNull
    private static QueryDocumentSnapshot toQueryDocumentSnapshot(@NonNull Query query,
                                                                @NonNull OfflineCachedDocument cachedDocument,
                                                                long index,
                                                                boolean isFromCache) {
        HashMap<String, Object> data = toHashMap(cachedDocument.getData());
        List<String> pathSegments = splitPath(cachedDocument.getPath());
        String documentId = pathSegments.isEmpty() ? cachedDocument.getPath() :
                pathSegments.get(pathSegments.size() - 1);
        String updateTime = toTimestampString(cachedDocument.getUpdateTimeMillis());
        String readTime = toTimestampString(cachedDocument.getReadTimeMillis());
        String version = cachedDocument.getVersion().isEmpty() ?
                TransactionOperation.DEFAULT_VERSION : cachedDocument.getVersion();
        Document document = new Document(data, documentId, readTime, updateTime, version, "",
                index, pathSegments, "");
        DocumentReference reference = buildDocumentReference(query, cachedDocument.getPath());
        return new QueryDocumentSnapshot(query.oradb, document, reference, cachedDocument.getPath(),
                isFromCache, cachedDocument.hasLocalMutations());
    }

    @NonNull
    private static DocumentReference buildDocumentReference(@NonNull Query query,
                                                           @NonNull String documentPath) {
        StringBuilder currentPath = new StringBuilder();
        CollectionReference parentCollection = null;
        DocumentReference documentReference = null;
        List<String> segments = splitPath(documentPath);
        for (int i = 0; i < segments.size(); i++) {
            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(segments.get(i));
            if (i % 2 == 0) {
                parentCollection = new CollectionReference(currentPath.toString(), documentReference,
                        query.oradb);
            } else {
                documentReference = new DocumentReference(currentPath.toString(), parentCollection,
                        query.oradb, true);
            }
        }
        if (documentReference == null) {
            throw new IllegalArgumentException("Invalid cached document path: " + documentPath);
        }
        return documentReference;
    }

    @NonNull
    private static Comparator<QueryDocumentSnapshot> getComparator(@NonNull Query query) {
        return (left, right) -> {
            for (OrderBy orderBy : query.explicitOrder) {
                Object leftValue = getFieldValue(left.getDocument().getData(), left.getId(),
                        orderBy.getField());
                Object rightValue = getFieldValue(right.getDocument().getData(), right.getId(),
                        orderBy.getField());
                int result = compareNullableValues(leftValue, rightValue);
                if (result != 0) {
                    return orderBy.getDirection() == Query.Direction.DESCENDING ? -result : result;
                }
            }
            return left.getId().compareTo(right.getId());
        };
    }

    private static boolean hasPendingWrites(@NonNull List<QueryDocumentSnapshot> snapshots) {
        for (QueryDocumentSnapshot snapshot : snapshots) {
            if (snapshot.getMetadata().hasPendingWrites()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @NonNull
    private static List<QueryDocumentSnapshot> reindex(@NonNull Query query,
                                                        @NonNull List<QueryDocumentSnapshot> snapshots) {
        List<QueryDocumentSnapshot> reindexed = new ArrayList<>();
        for (int index = 0; index < snapshots.size(); index++) {
            QueryDocumentSnapshot snapshot = snapshots.get(index);
            Document oldDocument = snapshot.getDocument();
            Document document = new Document(oldDocument.getData(), oldDocument.getId(),
                    oldDocument.getCreated(), oldDocument.getLastModified(), oldDocument.getVersion(),
                    oldDocument.getAsof(), (long) index, oldDocument.getPathSegments(), oldDocument.getRowId());
            reindexed.add(new QueryDocumentSnapshot(query.oradb, document, snapshot.getReference(),
                    snapshot.getReference().getPath(), snapshot.getMetadata().isFromCache(),
                    snapshot.getMetadata().hasPendingWrites()));
        }
        return reindexed;
    }

    @NonNull
    private static String getDocumentId(@NonNull OfflineCachedDocument cachedDocument) {
        List<String> segments = splitPath(cachedDocument.getPath());
        return segments.isEmpty() ? cachedDocument.getPath() : segments.get(segments.size() - 1);
    }

    @Nullable
    private static Object getFieldValue(@Nullable Map<String, Object> documentData,
                                        @NonNull String documentId,
                                        @Nullable FieldPath fieldPath) {
        if (fieldPath == null) {
            return null;
        }
        if (fieldPath.equals(FieldPath.documentId())) {
            return documentId;
        }
        Object current = documentData;
        for (String segment : fieldPath.getSegments()) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(segment);
        }
        return current;
    }

    private static int compareValues(@Nullable Object left, @Nullable Object right) {
        if (left == null || right == null) {
            return left == right ? 0 : left == null ? -1 : 1;
        }
        if (left instanceof Number && right instanceof Number) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
        }
        if (left instanceof Comparable && left.getClass().isInstance(right)) {
            return ((Comparable<Object>) left).compareTo(right);
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    private static int compareNullableValues(@Nullable Object left, @Nullable Object right) {
        return compareValues(left, right);
    }

    private static boolean likeMatches(@NonNull String actual, @NonNull String pattern) {
        String regex = Pattern.quote(pattern)
                .replace("%", "\\E.*\\Q")
                .replace("_", "\\E.\\Q");
        return Pattern.compile(regex, Pattern.DOTALL).matcher(actual).matches();
    }

    @NonNull
    private static List<String> splitPath(@NonNull String path) {
        List<String> segments = new ArrayList<>();
        if (path.isEmpty()) {
            return segments;
        }
        Collections.addAll(segments, path.split("/"));
        return segments;
    }

    @NonNull
    private static HashMap<String, Object> toHashMap(@Nullable Map<String, Object> source) {
        HashMap<String, Object> copy = new HashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), copyValue(entry.getValue()));
        }
        return copy;
    }

    @Nullable
    private static Object copyValue(@Nullable Object value) {
        if (value instanceof Map) {
            HashMap<String, Object> copy = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                copy.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<?>) value) {
                copy.add(copyValue(item));
            }
            return copy;
        }
        return value;
    }

    @NonNull
    private static String toTimestampString(@Nullable Long millis) {
        return millis == null ? "" : String.valueOf(millis);
    }
}
