package dev.siea.discord2fa.bungeecord.command;

import dev.siea.discord2fa.bungeecord.player.BungeeProxyPlayer;
import dev.siea.discord2fa.proxyserver.ProxyServer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * BungeeCord command that passes through Discord2FA commands (link, unlink) to the core.
 * Uses permissions discord2fa.link and discord2fa.unlink (default allow; revoke to deny).
 */
public final class Discord2FACommand extends Command implements TabExecutor {

    private static final String PERM_LINK = "discord2fa.link";
    private static final String PERM_UNLINK = "discord2fa.unlink";

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
            sender.sendMessage(TextComponent.fromLegacyText("§cThis command can only be executed by players."));
            return;
        }
        String label = getName();
        if ("link".equals(label) && !sender.hasPermission(PERM_LINK)) {
            sender.sendMessage(TextComponent.fromLegacyText("§cYou do not have permission to use this command."));
            return;
        }
        if ("unlink".equals(label) && !sender.hasPermission(PERM_UNLINK)) {
            sender.sendMessage(TextComponent.fromLegacyText("§cYou do not have permission to use this command."));
            return;
        }
        List<String> argsList = args == null ? List.of() : Arrays.asList(args);
        server.handleCommand(new BungeeProxyPlayer(player, proxy), label, argsList);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Registers all handled commands (link, unlink) and the admin command (discord2fa, d2fa) with the proxy.
     */
    public static void register(Plugin plugin, ProxyServer server, net.md_5.bungee.api.ProxyServer proxy) {
        for (String name : dev.siea.discord2fa.common.server.BaseServer.HANDLED_COMMANDS) {
            proxy.getPluginManager().registerCommand(plugin, new Discord2FACommand(name, server, proxy));
        }
        proxy.getPluginManager().registerCommand(plugin, new Discord2FAAdminCommand("discord2fa", server));
        proxy.getPluginManager().registerCommand(plugin, new Discord2FAAdminCommand("d2fa", server));
    }
}
