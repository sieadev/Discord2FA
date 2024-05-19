package dev.siea.common.util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;


public class UpdateChecker {
    private static final String RELEASES_URL = "https://api.github.com/repos/sieadev/discord2fa/releases";

    public static String generateUpdateMessage(String v) {
        String currentVersion = "V" + v;
        int releasesBehind = getReleasesBehind(currentVersion);
        String message = null;
        if (releasesBehind > 0) {
            message = ("You are " + releasesBehind + " release(s) behind! Download the newest release at https://modrinth.com/plugin/discord2fa");
        }
        return message;
    }

    public static String generateUpdateMessageColored(String v) {
        String currentVersion = "V" + v;
        int releasesBehind = getReleasesBehind(currentVersion);
        if (releasesBehind > 0) {
            return ("§cYou are §4" + releasesBehind + "§c release(s) behind! Download the newest release at https://modrinth.com/plugin/discord2fa");
        }
        else {
            return ("§eYou are running the latest release of Discord2FA! §e[" + currentVersion + "]");
        }
    }

    private static int getReleasesBehind(String currentVersion) {
        try {
            int behind = 0;
            JsonArray releases = getAllReleases();
            if (releases != null) {
                for (JsonElement release : releases) {
                    String tagName = release.getAsJsonObject().get("tag_name").getAsString();
                    if (!tagName.equalsIgnoreCase(currentVersion)) behind++;
                    else return behind;
                }
                return behind;
            }
        } catch (IOException ignore) {
        }
        return 0;
    }

    private static JsonArray getAllReleases() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(RELEASES_URL)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return JsonParser.parseString(response.body().string()).getAsJsonArray();
            } else {
                return null;
            }
        }
    }
}
