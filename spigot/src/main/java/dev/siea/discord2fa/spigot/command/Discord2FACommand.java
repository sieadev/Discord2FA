package dev.siea.discord2fa.spigot.command;

import dev.siea.discord2fa.common.server.BaseServer;
import dev.siea.discord2fa.gameserver.server.GameServer;
import dev.siea.discord2fa.spigot.player.SpigotPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Passes through Discord2FA commands (link, unlink) to the core. Only players can run these commands.
 * Tab completion returns no suggestions (link code is secret, unlink has no arguments).
 */
public final class Discord2FACommand implements CommandExecutor, TabCompleter {

    private final GameServer server;

    public Discord2FACommand(GameServer server) {
        this.server = server;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        List<String> argsList = args.length == 0 ? List.of() : Arrays.asList(args);
        return server.handleCommand(new SpigotPlayer(player), label, argsList);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
