package dev.siea.discord2fa.bungeecord.command;

import dev.siea.discord2fa.proxyserver.ProxyServer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Admin-only /discord2fa (alias /d2fa) with subcommands version and status.
 * Requires permission discord2fa.admin.
 */
public final class Discord2FAAdminCommand extends Command implements TabExecutor {

    private static final String PERMISSION = "discord2fa.admin";

    private final ProxyServer server;

    public Discord2FAAdminCommand(String name, ProxyServer server) {
        super(name);
        this.server = server;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(TextComponent.fromLegacyText("§cYou do not have permission to use this command."));
            return;
        }
        String sub = args.length > 0 ? args[0].trim().toLowerCase(java.util.Locale.ROOT) : "";
        List<String> lines;
        switch (sub) {
            case "version" -> lines = server.getVersionInfoMessage();
            case "status" -> lines = server.getStatusInfoMessage();
            default -> {
                sender.sendMessage(TextComponent.fromLegacyText("§eDiscord2FA §7- §f/discord2fa version §7| §f/discord2fa status"));
                return;
            }
        }
        for (String line : lines) {
            sender.sendMessage(TextComponent.fromLegacyText(line));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) return Collections.emptyList();
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase(java.util.Locale.ROOT) : "";
            return Stream.of("version", "status")
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        return Collections.emptyList();
    }
}
