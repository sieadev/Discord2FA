package dev.siea.discord2fa;

import dev.siea.discord2fa.commands.LinkCommand;
import dev.siea.discord2fa.commands.UnlinkCommand;
import dev.siea.discord2fa.database.Database;
import dev.siea.discord2fa.discord.DiscordBot;
import dev.siea.discord2fa.manager.VerifyManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.sql.SQLException;

public final class Discord2FA extends JavaPlugin {

    private static DiscordBot discordBot;
    private static Plugin plugin;
    private static boolean validLicense = false;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        loadConfig();
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
        getServer().getPluginManager().registerEvents(new VerifyManager(), this);
        getCommand("link").setExecutor(new LinkCommand());
        getCommand("unlink").setExecutor(new UnlinkCommand());
    }

    @Override
    public void onDisable() {
        DiscordBot.shutdown();
        try {
            Database.onDisable();
        } catch (SQLException ignore) {
        }
    }
    
    public static DiscordBot getDiscordBot() {
        return discordBot;
    }
    
    public static Plugin getPlugin() {
        return plugin;
    }

    private void loadConfig() {
        if (!validLicense) {
            getLogger().severe(String.format("[%s] - Disabled due to invalid license!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }
}
