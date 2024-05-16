package dev.siea.discord2fa;

import dev.siea.discord2fa.commands.LinkCommand;
import dev.siea.discord2fa.commands.UnlinkCommand;
import dev.siea.discord2fa.message.Messages;
import dev.siea.discord2fa.storage.StorageManager;
import dev.siea.discord2fa.storage.mysql.MySQLWrapper;
import dev.siea.discord2fa.discord.DiscordBot;
import dev.siea.discord2fa.manager.VerifyManager;
import dev.siea.discord2fa.util.UpdateChecker;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import java.sql.SQLException;
import java.util.Objects;

public final class Discord2FA extends JavaPlugin {
    private static Plugin plugin;

    @Override
    public void onEnable() {
        enablePlugin();
    }

    private void enablePlugin() {
        plugin = this;
        saveDefaultConfig();
        saveResource("lang/messages.yml", false);

        try{Messages.setLanguage(Objects.requireNonNull(getConfig().getString("language")));} catch (Exception ignore){}

        try{
            StorageManager.init(this);
        } catch (Exception e){
            disable("Disabling due to invalid Storage Type or connection failure! [THIS IS NOT A BUG DO NOT REPORT IT]");
            return;
        }

        try {
            new DiscordBot(this);
        } catch (Exception e) {
            disable("Disabling due to being unable to load Discord Bot! - " + e.getMessage());
            return;
        }
        getServer().getPluginManager().registerEvents(new VerifyManager(), this);
        getCommand("link").setExecutor(new LinkCommand());
        getCommand("unlink").setExecutor(new UnlinkCommand());
        enableBStats();
        new UpdateChecker(this);
    }

    private void enableBStats(){
        int pluginID = 21448;
        new Metrics(this, pluginID);
    }

    public static void disable(String reason){
        plugin.getLogger().severe(reason);
        try{
            if (MySQLWrapper.getConnection() != null) {
                MySQLWrapper.onDisable();
                plugin.getLogger().severe("Disabled Database wrapper...");
            }
        } catch (SQLException ignore) {
        }
        try{
            DiscordBot.shutdown();
            plugin.getLogger().severe("Disabled Discord Bot...");
        } catch (Exception ignore) {
        }
        plugin.getServer().getPluginManager().disablePlugin(plugin);
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
