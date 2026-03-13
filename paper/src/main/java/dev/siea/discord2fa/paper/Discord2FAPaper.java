package dev.siea.discord2fa.paper;

import dev.siea.discord2fa.common.logger.JulLoggerAdapter;
import dev.siea.discord2fa.common.i18n.LangLoader;
import dev.siea.discord2fa.common.versioning.BStats;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.gameserver.server.GameServer;
import dev.siea.discord2fa.paper.adapter.PaperConfigAdapter;
import dev.siea.discord2fa.paper.listener.Discord2FAEventListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;

public final class Discord2FAPaper extends JavaPlugin {

    private GameServer server;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PaperConfigAdapter configAdapter = new PaperConfigAdapter(getConfig());
        JulLoggerAdapter loggerAdapter = new JulLoggerAdapter(getLogger());
        MessageProvider messageProvider = loadMessages(configAdapter);
        server = new GameServer(configAdapter, loggerAdapter, messageProvider);
        getServer().getPluginManager().registerEvents(new Discord2FAEventListener(server), this);
        new Metrics(this, BStats.PLUGIN_ID);
    }

    private MessageProvider loadMessages(PaperConfigAdapter configAdapter) {
        try {
            return new LangLoader(configAdapter, Files.createDirectories(getDataFolder().toPath().resolve("lang")), this::getResource).load();
        } catch (IOException e) {
            getLogger().warning("Could not load lang file: " + e.getMessage());
            return LangLoader.loadFallback(this::getResource);
        }
    }
}
