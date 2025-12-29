package util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import config.ServerConfig;

/**
 * JSON parsing and serialization utility
 * Wraps Gson with validation
 */
public class JsonUtils {
    private static final Gson gson = new Gson();
    
    /**
     * Parses JSON string to JsonObject with size validation
     * @throws JsonSyntaxException if invalid JSON or too large
     */
    public static JsonObject parse(String json) throws JsonSyntaxException {
        if (!Validator.isValidJsonSize(json)) {
            throw new JsonSyntaxException("JSON size exceeds limit");
        }
        return JsonParser.parseString(json).getAsJsonObject();
    }
    
    /**
     * Converts object to JSON string
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
    
    /**
     * Checks if field exists and is not null
     */
    public static boolean hasField(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull();
    }
    
    /**
     * Safely gets string field or returns default
     */
    public static String getString(JsonObject obj, String field, String defaultValue) {
        if (hasField(obj, field)) {
            return obj.get(field).getAsString();
        }
        return defaultValue;
    }
    
    /**
     * Checks if required fields exist in JSON object
     */
    public static boolean hasRequiredFields(JsonObject obj, String... fields) {
        for (String field : fields) {
            if (!hasField(obj, field)) {
                return false;
            }
        }
        return true;
    }
}
