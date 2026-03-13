package dev.siea.discord2fa.gameserver.server;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.common.logger.LoggerAdapter;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.common.player.CommonPlayer;
import dev.siea.discord2fa.common.server.BaseServer;

public final class GameServer extends BaseServer {
    public GameServer(ConfigAdapter configProvider, LoggerAdapter logger, MessageProvider messageProvider) {
        super(configProvider, logger, messageProvider);
    }

    /**
     * To be called by the platform-specific game modules (Spigot/Paper) when a player joins.
     */
    public void handlePlayerJoin(CommonPlayer player) {
        addPlayer(player);
    }
}

