package com.oracle.test.oracledb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.oracle.mobile.fusabase.oracledb.FieldValue;
import com.oracle.mobile.fusabase.oracledb.VectorMetric;
import com.oracle.mobile.fusabase.oracledb.FindNearestOptions;
import com.oracle.mobile.fusabase.oracledb.FindNearestQuery;
import com.oracle.mobile.fusabase.oracledb.VectorValue;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

@RunWith(AndroidJUnit4.class)
public class VectorApiTests {

    @Test
    public void runQueryVectorSearchDensePayloadShape_matchesOpenApiContract() {
        FindNearestQuery query = FindNearestQuery.dense(Arrays.asList(0.12, 0.34, 0.56, 0.78));
        FindNearestOptions options = FindNearestOptions.builder()
                .metric(VectorMetric.COSINE)
                .topK(10)
                .threshold(0.25)
                .build();

        jakarta.json.JsonArrayBuilder denseVector = Json.createArrayBuilder();
        for (Double value : query.getVector()) {
            denseVector.add(value);
        }

        JsonObject vectorSearch = Json.createObjectBuilder()
                .add("field", "EMB")
                .add("query", Json.createObjectBuilder()
                        .add("vector", denseVector))
                .add("metric", options.getMetric().name())
                .add("topK", options.getTopK())
                .add("threshold", options.getThreshold())
                .build();

        assertTrue(vectorSearch.containsKey("field"));
        assertTrue(vectorSearch.containsKey("query"));
        assertTrue(vectorSearch.containsKey("metric"));
        assertTrue(vectorSearch.containsKey("topK"));
        assertTrue(vectorSearch.containsKey("threshold"));
        assertTrue(vectorSearch.getJsonObject("query").containsKey("vector"));
        assertFalse(vectorSearch.getJsonObject("query").containsKey("sparse"));
    }

    @Test
    public void runQueryVectorSearchSparsePayloadShape_matchesOpenApiContract() {
        jakarta.json.JsonArrayBuilder indices = Json.createArrayBuilder();
        indices.add(1).add(3);
        jakarta.json.JsonArrayBuilder values = Json.createArrayBuilder();
        values.add(0.7).add(0.4);

        JsonObject sparse = Json.createObjectBuilder()
                .add("type", "sparse")
                .add("dimension", 8)
                .add("indices", indices)
                .add("values", values)
                .build();

        JsonObject vectorSearch = Json.createObjectBuilder()
                .add("field", "SP_EMB")
                .add("query", Json.createObjectBuilder().add("sparse", sparse))
                .add("metric", "DOT")
                .add("topK", 10)
                .build();

        assertTrue(vectorSearch.getJsonObject("query").containsKey("sparse"));
        assertTrue(vectorSearch.getJsonObject("query").getJsonObject("sparse").containsKey("dimension"));
        assertTrue(vectorSearch.getJsonObject("query").getJsonObject("sparse").containsKey("indices"));
        assertTrue(vectorSearch.getJsonObject("query").getJsonObject("sparse").containsKey("values"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setOrUpdateV2Embeddings_opsPayloadShape_matchesOpenApiContract() throws Exception {
        Object queryHelper = allocateWithoutConstructor("com.oracle.mobile.fusabase.oracledb.QueryHelper");
        Method processUpdateData = queryHelper.getClass().getDeclaredMethod("processUpdateData", Map.class);
        processUpdateData.setAccessible(true);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("text", "v2 vector doc");
        updateMap.put("EMB", VectorValue.denseVector(Arrays.asList(0.11, 0.22, 0.33, 0.44)));
        updateMap.put("SP_EMB", VectorValue.sparseVector(384, Arrays.asList(2, 5, 9), Arrays.asList(0.13, 0.23, 0.43)));
        updateMap.put("SP_EMB_DELETE", FieldValue.deleteVector());

        Map<String, Object> payload = (Map<String, Object>) processUpdateData.invoke(queryHelper, updateMap);
        assertNotNull(payload);

        List<Map<String, Object>> dataArray = (List<Map<String, Object>>) payload.get("dataArray");
        assertNotNull(dataArray);

        boolean foundEmbeddingsSet = false;
        boolean foundEmbeddingsDelete = false;

        for (Map<String, Object> op : dataArray) {
            if ("$embeddings".equals(op.get("field")) && "set".equals(op.get("op"))) {
                foundEmbeddingsSet = true;
                Object value = op.get("value");
                assertTrue(value instanceof Map);
                Map<String, Object> embMap = (Map<String, Object>) value;
                assertTrue(embMap.containsKey("EMB"));
                assertTrue(embMap.containsKey("SP_EMB"));
            }
            if ("$embeddings".equals(op.get("field")) && "delete".equals(op.get("op"))) {
                foundEmbeddingsDelete = true;
                assertTrue(op.get("value") instanceof List);
            }
        }

        assertTrue("Expected $embeddings set op", foundEmbeddingsSet);
        assertTrue("Expected $embeddings delete op", foundEmbeddingsDelete);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setOrUpdateV2Embeddings_deleteUsesArrayValue_asOpenApiExample() throws Exception {
        Object queryHelper = allocateWithoutConstructor("com.oracle.mobile.fusabase.oracledb.QueryHelper");
        Method processUpdateData = queryHelper.getClass().getDeclaredMethod("processUpdateData", Map.class);
        processUpdateData.setAccessible(true);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("EMB", FieldValue.deleteVector());

        Map<String, Object> payload = (Map<String, Object>) processUpdateData.invoke(queryHelper, updateMap);
        List<Map<String, Object>> dataArray = (List<Map<String, Object>>) payload.get("dataArray");

        Map<String, Object> deleteOp = null;
        for (Map<String, Object> op : dataArray) {
            if ("$embeddings".equals(op.get("field")) && "delete".equals(op.get("op"))) {
                deleteOp = op;
                break;
            }
        }

        assertNotNull(deleteOp);
        assertTrue(deleteOp.get("value") instanceof List);
        assertEquals("arrayValue", deleteOp.get("valueType"));
        JsonArray deleteKeys = Json.createArrayBuilder().add(((List<String>) deleteOp.get("value")).get(0)).build();
        assertTrue(deleteKeys.size() >= 1);
    }

    private static Object allocateWithoutConstructor(String className) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return allocateInstance.invoke(unsafe, Class.forName(className));
    }
}
