package dev.siea.discord2fa.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.plugin.Plugin;
import java.io.IOException;

public class UpdateChecker {
    private static final String RELEASES_URL = "https://api.github.com/repos/sieadev/discord2fa/releases";

    public UpdateChecker(Plugin plugin) {
        String currentVersion = plugin.getDescription().getVersion();
        int releasesBehind = getReleasesBehind(currentVersion);
        if (releasesBehind > 0) {
            plugin.getLogger().severe("You are " + releasesBehind + " releases behind! Download it at https://modrinth.com/plugin/discord2fa");
        }
    }

    private int getReleasesBehind(String currentVersion) {
        try {
            JsonArray releases = getAllReleases();
            if (releases != null) {
                for (JsonElement release : releases) {
                    String tagName = release.getAsJsonObject().get("tag_name").getAsString();
                    if (tagName.equalsIgnoreCase(currentVersion)) {
                        return 0;
                    }
                }
                return releases.size();
            }
        } catch (IOException ignore) {
        }
        return -1;
    }

    private JsonArray getAllReleases() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(RELEASES_URL)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonParser parser = new JsonParser();
                return parser.parse(response.body().string()).getAsJsonArray();
            } else {
                return null;
            }
        }
    }
}
