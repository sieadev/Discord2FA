package dev.siea.discord2fa.proxyserver;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.common.logger.LoggerAdapter;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.common.player.CommonPlayer;
import dev.siea.discord2fa.common.server.BaseServer;

import java.nio.file.Path;
import java.util.concurrent.Executor;

public final class ProxyServer extends BaseServer {
    /** Use this when the platform can run callbacks on the proxy thread. Pass dataFolder so SQLite uses the plugin folder. */
    public ProxyServer(ConfigAdapter configProvider, LoggerAdapter logger, MessageProvider messageProvider, Executor serverExecutor, Path dataFolder) {
        super(configProvider, logger, messageProvider, serverExecutor, dataFolder);
        ProxyTargetServers.initialize(configProvider);
    }

    /**
     * To be called by proxy platform modules (BungeeCord, Velocity) when a player joins the proxy.
     */
    public void handlePlayerJoin(CommonPlayer player) {
        addPlayer(player, null);
    }

    /**
     * Same as {@link #handlePlayerJoin(CommonPlayer)} but with a callback when the player is skipped
     * (e.g. not linked and forceLink off, or location remembered). Use this to send the player to the
     * post-verification server so they are not stuck on the verification server.
     */
    public void handlePlayerJoin(CommonPlayer player, Runnable onSkippedVerification) {
        addPlayer(player, onSkippedVerification);
    }
}


