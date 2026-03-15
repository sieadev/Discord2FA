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
            String latestRelease = null;
            int behind = 0;
            for (JsonElement el : releases) {
                JsonObject release = el.getAsJsonObject();
                String tagName = release.has("tag_name") ? release.get("tag_name").getAsString() : "";
                if (tagName.isEmpty()) continue;
                String tagNormalized = normalizeTag(tagName);
                if (latestRelease == null) latestRelease = tagNormalized;
                if (tagNormalized.equalsIgnoreCase(normalized)) {
                    return new UpdateCheckResult(behind, DOWNLOAD_URL);
                }
                behind++;
            }
            // No matching release: current might be newer (experimental) or much older
            if (latestRelease != null && compareVersions(normalized, latestRelease) > 0) {
                return new UpdateCheckResult(0, DOWNLOAD_URL, true, latestRelease);
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

    /**
     * Compare two version strings (e.g. "2.0.0", "2.1.0-SNAPSHOT").
     * @return negative if a &lt; b, 0 if equal, positive if a &gt; b
     */
    private static int compareVersions(String a, String b) {
        if (a == null || b == null) return 0;
        String[] aParts = splitVersion(a);
        String[] bParts = splitVersion(b);
        int max = Math.max(aParts.length, bParts.length);
        for (int i = 0; i < max; i++) {
            int aNum = i < aParts.length ? parseSegment(aParts[i]) : 0;
            int bNum = i < bParts.length ? parseSegment(bParts[i]) : 0;
            if (aNum != bNum) return Integer.compare(aNum, bNum);
        }
        return 0;
    }

    private static String[] splitVersion(String v) {
        if (v == null || v.isEmpty()) return new String[0];
        String stripped = v.split("-")[0].trim();
        if (stripped.isEmpty()) return new String[0];
        return stripped.split("\\.");
    }

    private static int parseSegment(String s) {
        if (s == null || s.isEmpty()) return 0;
        s = s.trim();
        int num = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) num = num * 10 + (c - '0');
            else break;
        }
        return num;
    }
}
