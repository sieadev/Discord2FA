package dev.siea.discord2fa.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import dev.siea.discord2fa.common.logger.JulLoggerAdapter;
import dev.siea.discord2fa.common.i18n.LangLoader;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.common.i18n.ResourceLoader;
import dev.siea.discord2fa.velocity.adapter.VelocityConfigAdapter;
import dev.siea.discord2fa.velocity.listener.Discord2FAEventListener;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

@Plugin(
    id = "discord2fa",
    name = "Discord2FA",
    version = "2.0.0",
    description = "Two-factor authentication via Discord",
    authors = {"sieadev"}
)
public class Discord2FAVelocity {

    private final com.velocitypowered.api.proxy.ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private dev.siea.discord2fa.proxyserver.ProxyServer server;

    @Inject
    public Discord2FAVelocity(com.velocitypowered.api.proxy.ProxyServer proxy, Logger logger, @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        proxy.getEventManager().register(this, this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            logger.warning("Could not create data directory: " + e.getMessage());
        }
        Path configPath = dataDirectory.resolve("config.yml");
        if (!Files.exists(configPath)) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) Files.copy(in, configPath);
            } catch (IOException e) {
                logger.warning("Could not save default config: " + e.getMessage());
            }
        }
        Map<String, Object> configMap = Collections.emptyMap();
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                Map<String, Object> loaded = new Yaml().load(in);
                if (loaded != null) configMap = loaded;
            } catch (IOException e) {
                logger.severe("Could not load config: " + e.getMessage());
            }
        }
        VelocityConfigAdapter configAdapter = new VelocityConfigAdapter(configMap);
        JulLoggerAdapter loggerAdapter = new JulLoggerAdapter(logger);
        MessageProvider messageProvider = loadMessages(configAdapter);
        server = new dev.siea.discord2fa.proxyserver.ProxyServer(configAdapter, loggerAdapter, messageProvider);
        proxy.getEventManager().register(this, new Discord2FAEventListener(server, proxy));
    }

    private MessageProvider loadMessages(VelocityConfigAdapter configAdapter) {
        try {
            Path langDir = Files.createDirectories(dataDirectory.resolve("lang"));
            ResourceLoader loader = path -> getClass().getResourceAsStream("/" + path);
            return new LangLoader(configAdapter, langDir, loader).load();
        } catch (IOException e) {
            logger.warning("Could not load lang file: " + e.getMessage());
            return LangLoader.loadFallback(path -> getClass().getResourceAsStream("/" + path));
        }
    }

    public dev.siea.discord2fa.proxyserver.ProxyServer getProxyServer() {
        return server;
    }
}
