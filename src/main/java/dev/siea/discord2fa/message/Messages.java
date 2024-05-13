package dev.siea.discord2fa.message;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;

public class Messages {
    private static final HashMap<String, String> messages = new HashMap<>();
    private static ConfigurationSection configUtil = new ConfigUtil(Discord2FA.getPlugin(), "lang/en.yml").getConfig();
    private static final ConfigurationSection backup = new ConfigUtil(Discord2FA.getPlugin(), "lang/messages.yml").getConfig();

    public static void setLanguage(String lang){
        Plugin plugin = Discord2FA.getPlugin();
        try{
            String path = "lang/" + lang.toLowerCase() + ".yml";
            plugin.saveResource(path, false);
            configUtil = new ConfigUtil(plugin, path).getConfig();
        } catch (Exception ignore){

        }
    }

    public static String get(String key) {
        return messages.computeIfAbsent(key, Messages::retrieveMessageFromConfig);
    }

    private static String retrieveMessageFromConfig(String key) {
        String retrievedMessage = configUtil.getString(key);
        if (retrievedMessage == null) {
            retrievedMessage = backup.getString(key);
        }
        if (retrievedMessage == null) {
            retrievedMessage = "§c§lThis is not a bug do not report it! §c[Discord2FA >> Messages.yml] The following message is either missing or not set: §e" + key;
        }
        return retrievedMessage.replace("&","§");
    }

    public static void reload() {
        messages.clear();
    }
}



