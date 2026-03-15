package dev.siea.discord2fa.spigot;

import dev.siea.discord2fa.common.i18n.LangLoader;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.common.logger.JulLoggerAdapter;
import dev.siea.discord2fa.common.versioning.BStats;
import dev.siea.discord2fa.common.server.BaseServer;
import dev.siea.discord2fa.gameserver.server.GameServer;
import dev.siea.discord2fa.spigot.adapter.BukkitConfigAdapter;
import dev.siea.discord2fa.spigot.command.Discord2FAAdminCommand;
import dev.siea.discord2fa.spigot.command.Discord2FACommand;
import dev.siea.discord2fa.spigot.listener.Discord2FAEventListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executor;

public final class Discord2FASpigot extends JavaPlugin {

    private GameServer server;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        BukkitConfigAdapter configAdapter = new BukkitConfigAdapter(getConfig());
        JulLoggerAdapter loggerAdapter = new JulLoggerAdapter(getLogger());
        MessageProvider messageProvider = loadMessages(configAdapter);
        Executor mainThread = r -> getServer().getScheduler().runTask(this, r);
        try {
            server = new GameServer(configAdapter, loggerAdapter, messageProvider, mainThread, getDataFolder().toPath());
        } catch (IllegalStateException e) {
            getLogger().severe(e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(new Discord2FAEventListener(server), this);
        Discord2FACommand commandExecutor = new Discord2FACommand(server);
        for (String name : BaseServer.HANDLED_COMMANDS) {
            org.bukkit.command.PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(commandExecutor);
                cmd.setTabCompleter(commandExecutor);
            }
        }
        org.bukkit.command.PluginCommand adminCmd = getCommand("discord2fa");
        if (adminCmd != null) {
            Discord2FAAdminCommand adminExecutor = new Discord2FAAdminCommand(server);
            adminCmd.setExecutor(adminExecutor);
            adminCmd.setTabCompleter(adminExecutor);
        }
        new Metrics(this, BStats.PLUGIN_ID);
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }

    private MessageProvider loadMessages(BukkitConfigAdapter configAdapter) {
        try {
            return new LangLoader(configAdapter, Files.createDirectories(getDataFolder().toPath().resolve("lang")), this::getResource).load();
        } catch (IOException e) {
            getLogger().warning("Could not load lang file: " + e.getMessage());
            return LangLoader.loadFallback(this::getResource);
        }
    }
}
