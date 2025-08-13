package org.example.ssj3pj.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
 //JSON 형식 데이터를 원하는 형태로 파싱하기 위한 도구들
public final class JsonNodeUtils {

    private JsonNodeUtils() {}

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // ================== 기존 메서드 ==================

    public static String getText(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.path(key);
        return (v.isMissingNode() || v.isNull()) ? null : v.asText();
    }

    public static Integer getInt(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.path(key);
        if (v.isMissingNode() || v.isNull()) return null;
        if (v.isInt()) return v.asInt();
        if (v.isNumber()) return v.numberValue().intValue();
        if (v.isTextual()) {
            try { return Integer.parseInt(v.asText()); } catch (Exception ignored) {}
        }
        return null;
    }

    public static Long getLong(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.path(key);
        if (v.isMissingNode() || v.isNull()) return null;
        if (v.isLong()) return v.asLong();
        if (v.isNumber()) return v.numberValue().longValue();
        if (v.isTextual()) {
            try { return Long.parseLong(v.asText()); } catch (Exception ignored) {}
        }
        return null;
    }

    public static Double getDouble(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.path(key);
        if (v.isMissingNode() || v.isNull()) return null;
        if (v.isDouble()) return v.asDouble();
        if (v.isNumber()) return v.numberValue().doubleValue();
        if (v.isTextual()) {
            try { return Double.parseDouble(v.asText()); } catch (Exception ignored) {}
        }
        return null;
    }

    public static Boolean getBool(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.path(key);
        if (v.isMissingNode() || v.isNull()) return null;
        if (v.isBoolean()) return v.asBoolean();
        if (v.isTextual()) {
            String t = v.asText().toLowerCase(Locale.ROOT);
            if ("true".equals(t)) return true;
            if ("false".equals(t)) return false;
        }
        return null;
    }

    /** created_utc 같은 epoch(초 또는 소수) -> ISO-8601 문자열 */
    public static String epochToIso(JsonNode epochNode) {
        if (epochNode == null || epochNode.isMissingNode() || epochNode.isNull()) return null;
        try {
            double epoch = epochNode.isNumber() ? epochNode.asDouble()
                    : epochNode.isTextual() ? Double.parseDouble(epochNode.asText())
                    : Double.NaN;
            if (Double.isNaN(epoch)) return null;
            long seconds = (long) Math.floor(epoch);
            long nanos   = (long) Math.round((epoch - seconds) * 1_000_000_000L);
            return Instant.ofEpochSecond(seconds, nanos).atOffset(ZoneOffset.UTC).format(ISO);
        } catch (Exception e) {
            return null;
        }
    }

    /** epoch(초/소수) double → ISO-8601 */
    public static String epochToIso(double epoch) {
        long seconds = (long) Math.floor(epoch);
        long nanos   = (long) Math.round((epoch - seconds) * 1_000_000_000L);
        return Instant.ofEpochSecond(seconds, nanos).atOffset(ZoneOffset.UTC).format(ISO);
    }

    // ================== 추가 메서드 (helpers) ==================

    /** 여러 후보 중 먼저 존재하는 노드 반환 */
    public static JsonNode coalesce(JsonNode... nodes) {
        if (nodes == null) return missing();
        for (JsonNode n : nodes) {
            if (n != null && !n.isMissingNode() && !n.isNull()) return n;
        }
        return missing();
    }

    /** 배열의 첫 요소 반환 (없으면 missing) */
    public static JsonNode first(JsonNode arr) {
        if (arr != null && arr.isArray() && arr.size() > 0) return arr.get(0);
        return missing();
    }

    /** node[key]를 문자열로 반환 (없으면 null) */
    public static String text(JsonNode node, String key) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        JsonNode v = node.path(key);
        if (v.isMissingNode() || v.isNull()) return null;
        return v.asText(null);
    }

    /** 두 노드/키 후보 중 먼저 존재하는 텍스트 값 반환 */
    public static String firstText(JsonNode node1, String key1, JsonNode node2, String key2) {
        String v1 = text(node1, key1);
        if (v1 != null) return v1;
        return text(node2, key2);
    }

    /** 하나의 노드에서 여러 키 후보 중 첫 텍스트 값 반환 */
    public static String firstText(JsonNode node, String... keys) {
        if (keys == null) return null;
        for (String k : keys) {
            String v = text(node, k);
            if (v != null) return v;
        }
        return null;
    }

    /** MissingNode 싱글턴 반환 */
    public static JsonNode missing() {
        return MissingNode.getInstance();
    }
}
