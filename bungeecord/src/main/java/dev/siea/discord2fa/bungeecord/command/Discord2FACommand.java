package dev.siea.discord2fa.bungeecord.command;

import dev.siea.discord2fa.bungeecord.player.BungeeProxyPlayer;
import dev.siea.discord2fa.proxyserver.ProxyServer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * BungeeCord command that passes through Discord2FA commands (link, unlink) to the core.
 * Register one instance per command name via {@link #register(Plugin, ProxyServer, net.md_5.bungee.api.ProxyServer)}.
 * Tab completion returns no suggestions (link code is secret, unlink has no arguments).
 */
public final class Discord2FACommand extends Command implements TabExecutor {

    private final ProxyServer server;
    private final net.md_5.bungee.api.ProxyServer proxy;

    public Discord2FACommand(String name, ProxyServer server, net.md_5.bungee.api.ProxyServer proxy) {
        super(name);
        this.server = server;
        this.proxy = proxy;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof net.md_5.bungee.api.connection.ProxiedPlayer player)) {
            return;
        }
        List<String> argsList = args == null ? List.of() : Arrays.asList(args);
        server.handleCommand(new BungeeProxyPlayer(player, proxy), getName(), argsList);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Registers all handled commands (link, unlink) with the proxy.
     */
    public static void register(Plugin plugin, ProxyServer server, net.md_5.bungee.api.ProxyServer proxy) {
        for (String name : dev.siea.discord2fa.common.server.BaseServer.HANDLED_COMMANDS) {
            proxy.getPluginManager().registerCommand(plugin, new Discord2FACommand(name, server, proxy));
        }
    }
}
