package dev.siea.discord2fa.proxyserver;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.common.logger.LoggerAdapter;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.common.player.CommonPlayer;
import dev.siea.discord2fa.common.server.BaseServer;

import java.util.concurrent.Executor;

public final class ProxyServer extends BaseServer {
    /** Use this when the platform can run callbacks on the proxy thread (e.g. Bungee/Velocity scheduler). */
    public ProxyServer(ConfigAdapter configProvider, LoggerAdapter logger, MessageProvider messageProvider, Executor serverExecutor) {
        super(configProvider, logger, messageProvider, serverExecutor);
        ProxyTargetServers.initialize(configProvider);
    }

    /**
     * To be called by proxy platform modules (BungeeCord, Velocity) when a player joins the proxy.
     */
    public void handlePlayerJoin(CommonPlayer player) {
        addPlayer(player);
    }
}


