package dev.siea.discord2fa.common.i18n;

import dev.siea.discord2fa.common.config.ConfigAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads the language file specified in config (key "language"), copies bundled
 * lang files to disk so users can edit them (like config.yml), and provides a
 * MessageProvider for that language.
 */
public final class LangLoader {

    /** Config key for the language code (e.g. "en", "de"). */
    public static final String CONFIG_LANGUAGE_KEY = "language";

    /** Bundled language codes shipped in the jar. Each has a resource at lang/{code}.yml */
    public static final List<String> BUNDLED_LANGS = Collections.unmodifiableList(
        Arrays.asList("default", "en", "de", "fr", "it", "pl", "ro", "rs", "tr", "ua")
    );

    /** Fallback file used when a key is missing in the selected language. */
    public static final String DEFAULT_LANG_FILE = "default.yml";

    private final ConfigAdapter config;
    private final Path langDirectory;
    private final ResourceLoader resourceLoader;

    public LangLoader(ConfigAdapter config, Path langDirectory, ResourceLoader resourceLoader) {
        this.config = config;
        this.langDirectory = langDirectory;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Ensures lang directory exists, copies any bundled lang file to disk if it
     * doesn't exist, then loads the language from config and returns a MessageProvider.
     * Missing keys are resolved from default.yml, then from the optional default value, then the key itself.
     * If the configured language file is missing or invalid, falls back to en.yml then default.yml.
     */
    public MessageProvider load() throws IOException {
        Files.createDirectories(langDirectory);

        for (String code : BUNDLED_LANGS) {
            Path target = langDirectory.resolve(code + ".yml");
            if (!Files.exists(target)) {
                String resourcePath = "lang/" + code + ".yml";
                try (InputStream in = resourceLoader.getResource(resourcePath)) {
                    if (in != null) {
                        Files.copy(in, target);
                    }
                }
            }
        }

        Map<String, String> defaultMap = loadLangFile(langDirectory.resolve(DEFAULT_LANG_FILE));

        String lang = config.getString(CONFIG_LANGUAGE_KEY);
        if (lang == null || lang.trim().isEmpty()) lang = "en";
        lang = lang.trim().toLowerCase(Locale.ROOT);

        Path langFile = langDirectory.resolve(lang + ".yml");
        Map<String, String> messages = loadLangFile(langFile);
        if (messages.isEmpty() && !"en".equals(lang)) {
            messages = loadLangFile(langDirectory.resolve("en.yml"));
        }
        if (messages.isEmpty()) {
            messages = defaultMap;
        }

        Map<String, String> finalMessages = messages;
        Map<String, String> finalDefault = defaultMap;
        return (key, defaultValue) -> {
            String v = finalMessages.get(key);
            if (v != null && !v.isEmpty()) return v;
            v = finalDefault.get(key);
            if (v != null && !v.isEmpty()) return v;
            return defaultValue != null ? defaultValue : key;
        };
    }

    /**
     * Builds a MessageProvider that uses only default.yml from the jar (for when load() fails).
     * Uses no hardcoded strings; missing keys return the key.
     */
    public static MessageProvider loadFallback(ResourceLoader resourceLoader) {
        Map<String, String> defaultMap = new HashMap<>();
        try (InputStream in = resourceLoader.getResource("lang/" + DEFAULT_LANG_FILE)) {
            if (in != null) {
                Object raw = new org.yaml.snakeyaml.Yaml().load(in);
                if (raw instanceof Map) {
                    flatten("", (Map<String, Object>) raw, defaultMap);
                }
            }
        } catch (IOException ignored) {
        }
        Map<String, String> finalDefault = defaultMap;
        return (key, defaultValue) -> {
            String v = finalDefault.get(key);
            if (v != null && !v.isEmpty()) return v;
            return defaultValue != null ? defaultValue : key;
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> loadLangFile(Path path) throws IOException {
        if (!Files.exists(path)) return Collections.emptyMap();
        Object raw;
        try (InputStream in = Files.newInputStream(path)) {
            raw = new org.yaml.snakeyaml.Yaml().load(in);
        }
        if (!(raw instanceof Map)) return Collections.emptyMap();
        Map<String, String> flat = new HashMap<>();
        flatten("", (Map<String, Object>) raw, flat);
        return flat;
    }

    private static void flatten(String prefix, Map<String, Object> map, Map<String, String> out) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            Object val = e.getValue();
            if (val instanceof Map) {
                flatten(key, (Map<String, Object>) val, out);
            } else if (val != null) {
                out.put(key, val.toString());
            }
        }
    }
}
