package dev.siea.discord2fa.gameserver.server;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.common.logger.LoggerAdapter;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.common.player.CommonPlayer;
import dev.siea.discord2fa.common.server.BaseServer;

import java.nio.file.Path;
import java.util.concurrent.Executor;

public final class GameServer extends BaseServer {
    /** Use this when the platform can run callbacks on the server/main thread (e.g. Bukkit scheduler). Pass dataFolder so SQLite uses the plugin folder. */
    public GameServer(ConfigAdapter configProvider, LoggerAdapter logger, MessageProvider messageProvider, Executor serverExecutor, Path dataFolder) {
        super(configProvider, logger, messageProvider, serverExecutor, dataFolder);
    }

    /**
     * To be called by the platform-specific game modules (Spigot/Paper) when a player joins.
     */
    public void handlePlayerJoin(CommonPlayer player) {
        addPlayer(player);
    }
}

