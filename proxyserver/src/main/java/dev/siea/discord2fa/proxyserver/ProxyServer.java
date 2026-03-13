package dev.siea.discord2fa.proxyserver;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.common.logger.LoggerAdapter;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.common.player.CommonPlayer;
import dev.siea.discord2fa.common.server.BaseServer;

public final class ProxyServer extends BaseServer {

    public ProxyServer(ConfigAdapter configProvider, LoggerAdapter logger, MessageProvider messageProvider) {
        super(configProvider, logger, messageProvider);
        ProxyTargetServers.initialize(configProvider);
    }

    /**
     * To be called by proxy platform modules (BungeeCord, Velocity) when a player joins the proxy.
     */
    public void handlePlayerJoin(CommonPlayer player) {
        addPlayer(player);
    }
}


