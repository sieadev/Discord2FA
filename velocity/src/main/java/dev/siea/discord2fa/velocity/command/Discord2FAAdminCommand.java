package dev.siea.discord2fa.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import dev.siea.discord2fa.proxyserver.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Admin-only /discord2fa (alias /d2fa) with subcommands version and status.
 * Requires permission discord2fa.admin.
 */
public final class Discord2FAAdminCommand implements SimpleCommand {

    private static final String PERMISSION = "discord2fa.admin";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final ProxyServer server;

    public Discord2FAAdminCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission(PERMISSION)) {
            invocation.source().sendMessage(LEGACY.deserialize("§cYou do not have permission to use this command."));
            return;
        }
        String[] args = invocation.arguments();
        String sub = args != null && args.length > 0 ? args[0].trim().toLowerCase(java.util.Locale.ROOT) : "";
        List<String> lines;
        switch (sub) {
            case "version" -> lines = server.getVersionInfoMessage();
            case "status" -> lines = server.getStatusInfoMessage();
            default -> {
                invocation.source().sendMessage(LEGACY.deserialize("§eDiscord2FA §7- §f/discord2fa version §7| §f/discord2fa status"));
                return;
            }
        }
        for (String line : lines) {
            invocation.source().sendMessage(LEGACY.deserialize(line));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!invocation.source().hasPermission(PERMISSION)) return Collections.emptyList();
        String[] args = invocation.arguments();
        if (args != null && args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase(java.util.Locale.ROOT) : "";
            return Arrays.asList("version", "status").stream()
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        return Collections.emptyList();
    }
}
