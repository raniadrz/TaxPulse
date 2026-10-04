package gr.taxpulse.ai.prompt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny fluent builder for the JSON Schemas passed to Ollama's {@code format} parameter.
 * Keeps schemas readable and next to the records they deserialise into.
 */
public final class JsonSchemas {

    private JsonSchemas() {
    }

    public static Map<String, Object> string() {
        return Map.of("type", "string");
    }

    public static Map<String, Object> nullableString() {
        return Map.of("type", List.of("string", "null"));
    }

    public static Map<String, Object> nullableNumber() {
        return Map.of("type", List.of("number", "null"));
    }

    public static Map<String, Object> nullableEnum(List<String> values) {
        var all = new java.util.ArrayList<Object>(values);
        all.add(null);
        return Map.of("type", List.of("string", "null"), "enum", all);
    }

    public static Map<String, Object> arrayOf(Map<String, Object> items) {
        return Map.of("type", "array", "items", items);
    }

    /** Object schema whose properties are all required (models follow required fields more reliably). */
    public static Map<String, Object> object(Map<String, Map<String, Object>> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.copyOf(properties.keySet()));
        return schema;
    }

    /** Insertion-ordered property map (field order influences generation order). */
    @SafeVarargs
    public static Map<String, Map<String, Object>> props(Map.Entry<String, Map<String, Object>>... entries) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (var e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }
}
