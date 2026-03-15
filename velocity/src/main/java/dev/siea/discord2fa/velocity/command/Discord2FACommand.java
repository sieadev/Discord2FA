package dev.siea.discord2fa.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.siea.discord2fa.common.server.BaseServer;
import dev.siea.discord2fa.proxyserver.ProxyServer;
import dev.siea.discord2fa.velocity.player.VelocityProxyPlayer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Velocity command that passes through Discord2FA commands (link, unlink) to the core.
 * Tab completion returns no suggestions (link code is secret, unlink has no arguments).
 */
public final class Discord2FACommand implements SimpleCommand {

    private final ProxyServer discord2faServer;
    private final com.velocitypowered.api.proxy.ProxyServer velocityProxy;

    public Discord2FACommand(ProxyServer discord2faServer, com.velocitypowered.api.proxy.ProxyServer velocityProxy) {
        this.discord2faServer = discord2faServer;
        this.velocityProxy = velocityProxy;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
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
