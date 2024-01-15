package dev.siea.discord2fa;

import dev.siea.discord2fa.commands.LinkCommand;
import dev.siea.discord2fa.commands.UnlinkCommand;
import dev.siea.discord2fa.database.Database;
import dev.siea.discord2fa.discord.DiscordBot;
import dev.siea.discord2fa.manager.VerifyManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;

public final class Discord2FA extends JavaPlugin {

    private static DiscordBot discordBot;
    private static Plugin plugin;

    @Override
    public void onEnable() {
        enablePlugin();
    }

    private void enablePlugin() {
        plugin = this;
        saveDefaultConfig();
        if (!checkLicense()){
            getLogger().severe(String.format("[%s] - Disabled due to invalid license!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            discordBot = new DiscordBot(this);
        } catch (LoginException e) {
            getLogger().severe(String.format("[%s] - Disabled due to being unable to load Discord Bot!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            Database.onEnable(this);
        } catch (Exception e) {
            getLogger().severe(String.format("[%s] - Disabled due to being unable to load Database!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(new VerifyManager(), this);
        getCommand("link").setExecutor(new LinkCommand());
        getCommand("unlink").setExecutor(new UnlinkCommand());
    }

    private boolean checkLicense() {
        try {
            String license = getConfig().getString("license");
            if (license == null || license.isEmpty()) {
                getLogger().severe(String.format("[%s] - License is not set!", getDescription().getName()));
                return false;
            }
            HttpURLConnection connection = (HttpURLConnection) new URL("http://94.130.54.224:5000/validate-license").openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            String jsonInputString = "{\"license_key\": \"" + license + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonInputString.getBytes("utf-8"));
            }
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder responseStringBuilder = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        responseStringBuilder.append(line);
                    }
                    String response = responseStringBuilder.toString();
                    return response.contains("true");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onDisable() {
        try {
            DiscordBot.shutdown();
        } catch (Exception ignore) {
        }
        try {
            if (Database.getConnection() != null) return;
            Database.onDisable();
        } catch (SQLException ignore) {
        }
    }
    
    public static Plugin getPlugin() {
        return plugin;
    }
}
