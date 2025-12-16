package pseudopad.utils;

import com.google.gson.*;

/**
 * Utility class for formatting JSON content.
 * 
 * @author Geger John Paul Gabayeron
 */
public class JsonFormatter {
    private static final Gson PRETTY_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .create();

    /**
     * Formats a JSON string with proper indentation.
     * 
     * @param json Raw JSON string
     * @return Formatted JSON string, or original if invalid
     */
    public static String format(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }

        try {
            JsonElement element = JsonParser.parseString(json);
            return PRETTY_GSON.toJson(element);
        } catch (JsonSyntaxException e) {
            // Return original if parsing fails
            return json;
        }
    }

    /**
     * Minifies a JSON string by removing whitespace.
     * 
     * @param json Formatted JSON string
     * @return Minified JSON string, or original if invalid
     */
    public static String minify(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }

        try {
            JsonElement element = JsonParser.parseString(json);
            return new Gson().toJson(element);
        } catch (JsonSyntaxException e) {
            return json;
        }
    }

    /**
     * Validates if a string is valid JSON.
     * 
     * @param json String to validate
     * @return true if valid JSON, false otherwise
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }

        try {
            JsonParser.parseString(json);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
}
