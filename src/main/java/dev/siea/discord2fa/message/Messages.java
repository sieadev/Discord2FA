package dev.siea.discord2fa.message;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;

public class Messages {
    private static final HashMap<String, String> messages = new HashMap<>();
    private static Plugin plugin;
    private static final ConfigurationSection configUtil = new ConfigUtil(Discord2FA.getPlugin(), "messages.yml").getConfig();

    public static void onEnable(Plugin plugin){
        Messages.plugin = plugin;
    }
    public static String get(String key) {
        return messages.computeIfAbsent(key, Messages::retrieveMessageFromConfig);
    }

    private static String retrieveMessageFromConfig(String key) {
        String retrievedMessage = configUtil.getString(key);
        if (retrievedMessage == null) {
            retrievedMessage = "§c§lThis is not a bug do not report it! §c[Discord2FA >> Config.yml] The following message is either missing or not set: §e" + key;
        }
        return retrievedMessage.replace("&","§");
    }

    public static void reload() {
        messages.clear();
    }
}



