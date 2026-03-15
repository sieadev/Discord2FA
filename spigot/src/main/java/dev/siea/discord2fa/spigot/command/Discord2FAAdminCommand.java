package dev.siea.discord2fa.spigot.command;

import dev.siea.discord2fa.gameserver.server.GameServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Admin-only /discord2fa (alias /d2fa) with subcommands version and status.
 * Requires permission discord2fa.admin.
 */
public final class Discord2FAAdminCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "discord2fa.admin";

    private final GameServer server;

    public Discord2FAAdminCommand(GameServer server) {
        this.server = server;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        String sub = args.length > 0 ? args[0].trim().toLowerCase(java.util.Locale.ROOT) : "";
        List<String> lines;
        switch (sub) {
            case "version" -> lines = server.getVersionInfoMessage();
            case "status" -> lines = server.getStatusInfoMessage();
            default -> {
                sender.sendMessage("§eDiscord2FA §7- §f/discord2fa version §7| §f/discord2fa status");
                return true;
            }
        }
        for (String line : lines) {
            sender.sendMessage(line);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
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
