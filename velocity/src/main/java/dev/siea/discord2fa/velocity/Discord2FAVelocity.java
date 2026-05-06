package dev.siea.discord2fa.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import dev.siea.discord2fa.common.logger.JulLoggerAdapter;
import dev.siea.discord2fa.common.i18n.LangLoader;
import dev.siea.discord2fa.common.versioning.BStats;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.common.i18n.ResourceLoader;
import dev.siea.discord2fa.common.versioning.FastStats;
import dev.siea.discord2fa.velocity.adapter.VelocityConfigAdapter;
import dev.siea.discord2fa.velocity.command.Discord2FAAdminCommand;
import dev.siea.discord2fa.velocity.command.Discord2FACommand;
import dev.siea.discord2fa.velocity.listener.Discord2FAEventListener;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.Metrics;
import dev.faststats.velocity.VelocityMetrics;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

@Plugin(
    id = "discord2fa",
    name = "Discord2FA",
    version = GeneratedVersion.VERSION,
    description = "Two-factor authentication via Discord",
    authors = {"sieadev"}
)
public class Discord2FAVelocity {

    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();

    private final com.velocitypowered.api.proxy.ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final org.bstats.velocity.Metrics.Factory bstatsMetricsFactory;
    private final VelocityMetrics.Factory fastStatsMetricsFactory;

    private dev.siea.discord2fa.proxyserver.ProxyServer server;
    private @Nullable Metrics fastStatsMetrics = null;

    @Inject
    public Discord2FAVelocity(
            com.velocitypowered.api.proxy.ProxyServer proxy,
            Logger logger,
            @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory,
            org.bstats.velocity.Metrics.Factory bstatsMetricsFactory,
            VelocityMetrics.Factory fastStatsMetricsFactory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.bstatsMetricsFactory = bstatsMetricsFactory;
        this.fastStatsMetricsFactory = fastStatsMetricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            ERROR_TRACKER.trackError(e);
            logger.warning("Could not create data directory: " + e.getMessage());
        }
        Path configPath = dataDirectory.resolve("config.yml");
        if (!Files.exists(configPath)) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) Files.copy(in, configPath);
            } catch (IOException e) {
                ERROR_TRACKER.trackError(e);
                logger.warning("Could not save default config: " + e.getMessage());
            }
        }
        Map<String, Object> configMap = Collections.emptyMap();
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                Map<String, Object> loaded = new Yaml().load(in);
                if (loaded != null) configMap = loaded;
            } catch (IOException e) {
                ERROR_TRACKER.trackError(e);
                logger.severe("Could not load config: " + e.getMessage());
            }
        }
        VelocityConfigAdapter configAdapter = new VelocityConfigAdapter(configMap);
        JulLoggerAdapter loggerAdapter = new JulLoggerAdapter(logger);
        MessageProvider messageProvider = loadMessages(configAdapter);
        Executor proxyExecutor = r -> proxy.getScheduler().buildTask(this, r).schedule();
        try {
            server = new dev.siea.discord2fa.proxyserver.ProxyServer(configAdapter, loggerAdapter, messageProvider, proxyExecutor, dataDirectory);
        } catch (IllegalStateException e) {
            ERROR_TRACKER.trackError(e);
            logger.severe(e.getMessage());
            return;
        }
        proxy.getEventManager().register(this, new Discord2FAEventListener(server, proxy));
        Discord2FACommand.register(server, proxy, proxy.getCommandManager());
        proxy.getCommandManager().register("discord2fa", new Discord2FAAdminCommand(server), "d2fa");
        bstatsMetricsFactory.make(this, BStats.PLUGIN_ID);
        fastStatsMetrics = fastStatsMetricsFactory
                .token(FastStats.id)
                .errorTracker(ERROR_TRACKER)
                .create(this);
        fastStatsMetrics.ready();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (fastStatsMetrics != null) {
            fastStatsMetrics.shutdown();
            fastStatsMetrics = null;
        }
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }

    private MessageProvider loadMessages(VelocityConfigAdapter configAdapter) {
        try {
            Path langDir = Files.createDirectories(dataDirectory.resolve("lang"));
            ResourceLoader loader = path -> getClass().getResourceAsStream("/" + path);
            return new LangLoader(configAdapter, langDir, loader).load();
        } catch (IOException e) {
            ERROR_TRACKER.trackError(e);
            logger.warning("Could not load lang file: " + e.getMessage());
            return LangLoader.loadFallback(path -> getClass().getResourceAsStream("/" + path));
        }
    }
}
