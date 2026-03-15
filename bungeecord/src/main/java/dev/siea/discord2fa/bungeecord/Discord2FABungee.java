package dev.siea.discord2fa.bungeecord;

import dev.siea.discord2fa.bungeecord.adapter.BungeeConfigAdapter;
import dev.siea.discord2fa.bungeecord.command.Discord2FACommand;
import dev.siea.discord2fa.bungeecord.listener.Discord2FAEventListener;
import dev.siea.discord2fa.common.logger.JulLoggerAdapter;
import dev.siea.discord2fa.common.i18n.LangLoader;
import dev.siea.discord2fa.common.versioning.BStats;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.proxyserver.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.Metrics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.concurrent.Executor;

public final class Discord2FABungee extends Plugin {

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                if (in != null) Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                getLogger().warning("Could not save default config: " + e.getMessage());
            }
        }
        Configuration configuration;
        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            getLogger().severe("Could not load config: " + e.getMessage());
            configuration = null;
        }
        BungeeConfigAdapter configAdapter = new BungeeConfigAdapter(configuration);
        Logger logger = getLogger();
        JulLoggerAdapter loggerAdapter = new JulLoggerAdapter(logger);
        MessageProvider messageProvider = loadMessages(configAdapter);
        Executor proxyExecutor = r -> getProxy().getScheduler().runAsync(this, r);
        ProxyServer server;
        try {
            server = new ProxyServer(configAdapter, loggerAdapter, messageProvider, proxyExecutor, getDataFolder().toPath());
        } catch (IllegalStateException e) {
            getLogger().severe(e.getMessage());
            return;
        }
        getProxy().getPluginManager().registerListener(this, new Discord2FAEventListener(server, getProxy()));
        Discord2FACommand.register(this, server, getProxy());
        new Metrics(this, BStats.PLUGIN_ID);
    }

    private MessageProvider loadMessages(BungeeConfigAdapter configAdapter) {
        try {
            Path langDir = getDataFolder().toPath().resolve("lang");
            return new LangLoader(configAdapter, Files.createDirectories(langDir), this::getResourceAsStream).load();
        } catch (IOException e) {
            getLogger().warning("Could not load lang file: " + e.getMessage());
            return LangLoader.loadFallback(this::getResourceAsStream);
        }
    }
}
