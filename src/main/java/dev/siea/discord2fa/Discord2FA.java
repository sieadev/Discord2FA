package dev.siea.discord2fa;

import dev.siea.discord2fa.database.Database;
import dev.siea.discord2fa.discord.DiscordBot;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;

public final class Discord2FA extends JavaPlugin {

    private static DiscordBot discordBot;
    private static Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        try {
            discordBot = new DiscordBot(this);
        } catch (LoginException e) {
            getLogger().severe(String.format("[%s] - Disabled due to being unable to load Discord Bot", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            Database.onEnable(this);
        } catch (Exception e) {
            getLogger().severe(String.format("[%s] - Disabled due to being unable to load Database", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    
    public static DiscordBot getDiscordBot() {
        return discordBot;
    }
    
    public static Plugin getPlugin() {
        return plugin;
    }
}
