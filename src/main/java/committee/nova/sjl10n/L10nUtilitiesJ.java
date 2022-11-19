package committee.nova.sjl10n;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * A more generalized version of John-Paul-R's one
 */
public class L10nUtilitiesJ {
    private static final Gson GSON = new Gson();
    private static final Pattern TOKEN_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
    public static final String DEFAULT_LANGUAGE = "en_us";
    private static final Logger LOGGER = LogManager.getLogger();

    private static JsonText create(String modId, String langId) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        Objects.requireNonNull(builder);
        BiConsumer<String, String> biConsumer = builder::put;
        final String resourceFString = "/assets/%s/lang/%s.json";
        final String resourceLocation = String.format(resourceFString, modId, langId);
        try {
            InputStream inputStream = JsonText.class.getResourceAsStream(resourceLocation);
            if (inputStream == null) {
                LOGGER.info(String.format("No %s lang file for the language '%s' found. Make it to 'en_us' by default.", modId, langId));
                inputStream = JsonText.class.getResourceAsStream(String.format(resourceFString, modId, DEFAULT_LANGUAGE));
            }

            try {
                load(inputStream, biConsumer);
            } catch (Throwable var7) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (inputStream != null) {
                inputStream.close();
            }
        } catch (JsonParseException | IOException var8) {
            LOGGER.error("Couldn't read strings from {}", resourceLocation, var8);
        }

        final Map<String, String> map = builder.build();
        return new JsonText() {
            public String get(String key) {
                return map.getOrDefault(key, key);
            }
        };
    }

    public static void load(InputStream inputStream, BiConsumer<String, String> entryConsumer) {
        JsonObject jsonObject = GSON.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), JsonObject.class);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String string = TOKEN_PATTERN.matcher(convertToString(entry.getValue(), entry.getKey())).replaceAll("%$1s");
            entryConsumer.accept(entry.getKey(), string);
        }
    }

    public abstract static class JsonText {
        private JsonText() {
        }

        public abstract String get(String key);
    }

    private static String convertToString(JsonElement json, String str) {
        if (json.isJsonPrimitive()) {
            return json.getAsString();
        } else {
            throw new JsonSyntaxException("Expected " + str + " to be a string, was " + getType(json));
        }
    }

    private static String getType(@Nullable JsonElement json) {
        String s = StringUtils.abbreviateMiddle(String.valueOf(json), "...", 10);
        if (json == null) {
            return "null (missing)";
        } else if (json.isJsonNull()) {
            return "null (json)";
        } else if (json.isJsonArray()) {
            return "an array (" + s + ")";
        } else if (json.isJsonObject()) {
            return "an object (" + s + ")";
        } else {
            if (json.isJsonPrimitive()) {
                JsonPrimitive jsonprimitive = json.getAsJsonPrimitive();
                if (jsonprimitive.isNumber()) {
                    return "a number (" + s + ")";
                }

                if (jsonprimitive.isBoolean()) {
                    return "a boolean (" + s + ")";
                }
            }

            return s;
        }
    }
}
