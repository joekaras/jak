// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package ibsmas.vault.internal;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void parsesVaultKvResponse() {
        Map<String, Object> root = Json.asObject(Json.parse(
            " {\"data\":{\"data\":{\"user\":\"svc\",\"port\":443,\"ratio\":1.5,\"on\":true,\"none\":null,\"tags\":[\"a\",\"b\"]},"
                + "\"metadata\":{\"version\":3}}} "));

        Map<String, Object> data = Json.getObject(Json.getObject(root, "data"), "data");
        assertEquals("svc", data.get("user"));
        assertEquals(443L, data.get("port"));
        assertEquals(1.5, data.get("ratio"));
        assertEquals(Boolean.TRUE, data.get("on"));
        assertTrue(data.containsKey("none"));
        assertNull(data.get("none"));
        assertEquals(Arrays.asList("a", "b"), data.get("tags"));
    }

    @Test
    void decodesEscapes() {
        assertEquals("q\"b\\s/n\nt\tu\u00e9", Json.parse("\"q\\\"b\\\\s\\/n\\nt\\tu\\u00e9\""));
    }

    @Test
    void writesAndRoundTrips() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("role_id", "r\"1");
        map.put("ctrl", "a\u0001b");
        map.put("n", 5);
        map.put("list", Arrays.asList(1, "x"));

        String json = Json.write(map);

        assertEquals("{\"role_id\":\"r\\\"1\",\"ctrl\":\"a\\u0001b\",\"n\":5,\"list\":[1,\"x\"]}", json);
        Map<String, Object> back = Json.asObject(Json.parse(json));
        assertEquals("r\"1", back.get("role_id"));
        assertEquals("a\u0001b", back.get("ctrl"));
        List<?> list = (List<?>) back.get("list");
        assertEquals(2, list.size());
    }

    @Test
    void malformedInputErrorNeverEchoesContent() {
        for (String bad : new String[] {"{\"pw\":\"SECRET-CANARY\"", "{\"pw\" \"SECRET-CANARY\"}", "[1,]", "tru", "\"\\x\"", "{} extra"}) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Json.parse(bad), bad);
            assertFalse(ex.getMessage().contains("SECRET-CANARY"));
            assertTrue(ex.getMessage().startsWith("Invalid JSON at position"));
        }
    }

    @Test
    void deepNestingIsRejectedInsteadOfOverflowingTheStack() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append('[');
        }
        assertThrows(IllegalArgumentException.class, () -> Json.parse(sb.toString()));
    }
}
