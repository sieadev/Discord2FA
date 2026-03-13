package dev.siea.discord2fa.common.versioning;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Checks GitHub releases to determine if the current version is behind.
 * Does not generate or log messages; the server uses the result with its logger.
 */
public final class UpdateChecker {

    private static final String RELEASES_URL = "https://api.github.com/repos/sieadev/Discord2FA/releases";
    private static final String DOWNLOAD_URL = "https://modrinth.com/plugin/discord2fa";

    /**
     * Check how many releases behind the current version is.
     *
     * @param currentVersion plugin version string (e.g. "2.0.0" or "V2.0.0")
     * @return result with versions behind (0 = latest) and download URL; never null
     */
    public static UpdateCheckResult check() {
        String currentVersion = PluginVersion.get();
        if (currentVersion.isBlank()) return null;

        String normalized = normalizeTag(currentVersion);
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RELEASES_URL))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null) {
                return new UpdateCheckResult(0, DOWNLOAD_URL);
            }
            JsonArray releases = JsonParser.parseString(response.body()).getAsJsonArray();
            int behind = 0;
            for (JsonElement el : releases) {
                JsonObject release = el.getAsJsonObject();
                String tagName = release.has("tag_name") ? release.get("tag_name").getAsString() : "";
                if (tagName.isEmpty()) continue;
                String tagNormalized = normalizeTag(tagName);
                if (tagNormalized.equalsIgnoreCase(normalized)) {
                    return new UpdateCheckResult(behind, DOWNLOAD_URL);
                }
                behind++;
            }
            return new UpdateCheckResult(behind, DOWNLOAD_URL);
        } catch (IOException | InterruptedException e) {
            return new UpdateCheckResult(0, DOWNLOAD_URL);
        }
    }

    private static String normalizeTag(String v) {
        if (v == null) return "";
        String s = v.trim();
        if (s.toUpperCase().startsWith("V")) {
            s = s.substring(1).trim();
        }
        return s.isEmpty() ? v.trim() : s;
    }
}
