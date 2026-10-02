package dev.siea.discord2fa.common.i18n;

import dev.siea.discord2fa.common.testutil.MapConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LangLoaderTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final Path LANG_RESOURCES = Path.of("src/main/resources/lang");
    private static final ResourceLoader CLASSPATH = path -> LangLoaderTest.class.getClassLoader().getResourceAsStream(path);

    @TempDir
    Path langDir;

    static Stream<String> shippedLanguages() throws IOException {
        try (Stream<Path> files = Files.list(LANG_RESOURCES)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".yml"))
                    .map(n -> n.substring(0, n.length() - 4))
                    .sorted()
                    .toList()
                    .stream();
        }
    }

    /** Every message key the common code looks up. */
    static Set<String> keysUsedInCode() throws IOException {
        Pattern call = Pattern.compile("messageProvider\\.get\\(\"([^\"]+)\"\\)");
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = call.matcher(Files.readString(file));
                while (m.find()) keys.add(m.group(1));
            }
        }
        return keys;
    }

    private static Map<String, String> flatten(String language) throws IOException {
        java.util.HashMap<String, String> out = new java.util.HashMap<>();
        try (InputStream in = Files.newInputStream(LANG_RESOURCES.resolve(language + ".yml"))) {
            flatten("", new Yaml().load(in), out);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Object node, Map<String, String> out) {
        if (!(node instanceof Map<?, ?> map)) return;
        for (Map.Entry<String, Object> e : ((Map<String, Object>) map).entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            if (e.getValue() instanceof Map) flatten(key, e.getValue(), out);
            else if (e.getValue() != null) out.put(key, e.getValue().toString());
        }
    }

    @Test
    void everyShippedLanguageIsBundled() throws IOException {
        assertEquals(shippedLanguages().toList(), LangLoader.BUNDLED_LANGS.stream().sorted().toList(),
                "LangLoader.BUNDLED_LANGS must list exactly the files in resources/lang");
    }

    @Test
    void defaultFileDefinesEveryKeyTheCodeUses() throws IOException {
        Set<String> missing = new TreeSet<>(keysUsedInCode());
        missing.removeAll(flatten("default").keySet());

        assertFalse(keysUsedInCode().isEmpty());
        assertEquals(Set.of(), missing, "keys used in code but missing from default.yml");
    }

    @ParameterizedTest
    @MethodSource("shippedLanguages")
    void languageFileParsesAndKeepsPlaceholders(String language) throws IOException {
        Map<String, String> messages = flatten(language);
        Map<String, String> defaults = flatten("default");

        assertFalse(messages.isEmpty(), language + ".yml is empty or invalid");
        messages.forEach((key, value) -> assertFalse(value.contains("dev.siea"),
                language + ".yml '" + key + "' leaks a Java package name: " + value));
        for (Map.Entry<String, String> e : messages.entrySet()) {
            String reference = defaults.get(e.getKey());
            if (reference == null) continue;
            for (String placeholder : List.of("%code%", "%ip%", "%player%")) {
                if (reference.contains(placeholder)) {
                    assertTrue(e.getValue().contains(placeholder),
                            language + ".yml '" + e.getKey() + "' lost placeholder " + placeholder);
                }
            }
        }
    }

    @Test
    void loadCopiesAllBundledFilesAndUsesTheConfiguredLanguage() throws IOException {
        MessageProvider messages = new LangLoader(new MapConfig().set("language", " DE "), langDir, CLASSPATH).load();

        for (String code : LangLoader.BUNDLED_LANGS) {
            assertTrue(Files.exists(langDir.resolve(code + ".yml")), code + ".yml was not copied");
        }
        assertEquals(flatten("de").get("linkSuccess").replace('&', '§'), messages.get("linkSuccess"));
    }

    @Test
    void ampersandColorCodesAreTranslated() throws IOException {
        MessageProvider messages = new LangLoader(new MapConfig(), langDir, CLASSPATH).load();

        String value = messages.get("notLinked");
        assertFalse(value.contains("&"));
        assertTrue(value.contains("§"));
    }

    @Test
    void userEditsAreKeptAndMissingKeysFallBackToDefault() throws IOException {
        Files.createDirectories(langDir);
        Files.writeString(langDir.resolve("en.yml"), "linkSuccess: \"&aCustom!\"\n");

        MessageProvider messages = new LangLoader(new MapConfig().set("language", "en"), langDir, CLASSPATH).load();

        assertEquals("§aCustom!", messages.get("linkSuccess"));
        assertEquals(flatten("default").get("notLinked").replace('&', '§'), messages.get("notLinked"));
        assertEquals("no.such.key", messages.get("no.such.key"));
    }

    @Test
    void unknownLanguageFallsBackToEnglish() throws IOException {
        MessageProvider messages = new LangLoader(new MapConfig().set("language", "xx"), langDir, CLASSPATH).load();

        assertEquals(flatten("en").get("linkSuccess").replace('&', '§'), messages.get("linkSuccess"));
    }

    @Test
    void fallbackProviderReadsDefaultFromTheJar() {
        MessageProvider messages = LangLoader.loadFallback(CLASSPATH);

        assertEquals(flatten0("notLinked"), messages.get("notLinked"));
        assertEquals("no.such.key", messages.get("no.such.key"));
    }

    private static String flatten0(String key) {
        try {
            return flatten("default").get(key).replace('&', '§');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
