package dev.siea.discord2fa;

import dev.siea.discord2fa.commands.LinkCommand;
import dev.siea.discord2fa.commands.UnlinkCommand;
import dev.siea.discord2fa.message.Messages;
import dev.siea.discord2fa.storage.StorageManager;
import dev.siea.discord2fa.storage.mysql.MySQLWrapper;
import dev.siea.discord2fa.discord.DiscordBot;
import dev.siea.discord2fa.manager.VerifyManager;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import javax.security.auth.login.LoginException;
import java.sql.SQLException;

public final class Discord2FA extends JavaPlugin {
    private static Plugin plugin;

    @Override
    public void onEnable() {
        enablePlugin();
    }

    private void enablePlugin() {
        plugin = this;
        saveDefaultConfig();
        saveResource("messages.yml", false);
        Messages.onEnable(this);
        StorageManager.init(this);
        try {
            new DiscordBot(this);
        } catch (LoginException e) {
            getLogger().severe(String.format("[%s] - Disabled due to being unable to load Discord Bot!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(new VerifyManager(), this);
        getCommand("link").setExecutor(new LinkCommand());
        getCommand("unlink").setExecutor(new UnlinkCommand());
        enableBStats();
    }

    private void enableBStats(){
        int pluginID = 21448;
        new Metrics(this, pluginID);
    }

    @Override
    public void onDisable() {
        try {
            DiscordBot.shutdown();
        } catch (Exception ignore) {
        }
        try {
            if (MySQLWrapper.getConnection() != null) return;
            MySQLWrapper.onDisable();
        } catch (SQLException ignore) {
        }
    }
    
    public static Plugin getPlugin() {
        return plugin;
    }
}
