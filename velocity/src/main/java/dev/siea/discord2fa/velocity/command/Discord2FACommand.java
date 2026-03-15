package dev.siea.discord2fa.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.siea.discord2fa.common.server.BaseServer;
import dev.siea.discord2fa.proxyserver.ProxyServer;
import dev.siea.discord2fa.velocity.player.VelocityProxyPlayer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Velocity command that passes through Discord2FA commands (link, unlink) to the core.
 * All players can use link/unlink by default; use a permission plugin and revoke
 * discord2fa.link / discord2fa.unlink to restrict.
 */
public final class Discord2FACommand implements SimpleCommand {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final ProxyServer discord2faServer;
    private final com.velocitypowered.api.proxy.ProxyServer velocityProxy;

    public Discord2FACommand(ProxyServer discord2faServer, com.velocitypowered.api.proxy.ProxyServer velocityProxy) {
        this.discord2faServer = discord2faServer;
        this.velocityProxy = velocityProxy;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // allow all; link/unlink are player-only and checked in execute()
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(LEGACY.deserialize("§cThis command can only be executed by players."));
            return;
        }
        String alias = invocation.alias();
        String[] args = invocation.arguments();
        List<String> argsList = args == null ? List.of() : Arrays.asList(args);
        discord2faServer.handleCommand(new VelocityProxyPlayer(player, velocityProxy), alias, argsList);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return Collections.emptyList();
    }

    /**
     * Registers this command for all handled aliases (link, unlink) with the Velocity command manager.
     */
    public static void register(ProxyServer discord2faServer, com.velocitypowered.api.proxy.ProxyServer velocityProxy, com.velocitypowered.api.command.CommandManager velocityCommandManager) {
        Discord2FACommand command = new Discord2FACommand(discord2faServer, velocityProxy);
        List<String> names = BaseServer.HANDLED_COMMANDS;
        if (names.size() == 1) {
            velocityCommandManager.register(names.get(0), command);
        } else {
            velocityCommandManager.register(names.get(0), command, names.subList(1, names.size()).toArray(new String[0]));
        }
    }
}
