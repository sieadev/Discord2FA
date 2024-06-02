package dev.siea.spigot.commands;

import dev.siea.common.util.UpdateChecker;
import dev.siea.spigot.Discord2FA;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Discord2FACommand implements CommandExecutor {
    private final String version;
    private final Discord2FA plugin;

    public Discord2FACommand(Discord2FA plugin) {
        version = plugin.getDescription().getVersion();
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args){
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            long startTime = System.currentTimeMillis();
            plugin.reload();
            long duration = System.currentTimeMillis() - startTime;
            if (sender instanceof Player) {
                sender.sendMessage("§eReloaded Discord2FA in §6" + duration + "§e milliseconds!");
            } else {
                sender.sendMessage("Reloaded Discord2FA in " + duration + " milliseconds!");
            }
            return true;
        }
        if (sender instanceof Player player){
            player.sendMessage(UpdateChecker.generateUpdateMessageColored(version));
            return true;
        }
        else {
            String versionMessage = UpdateChecker.generateUpdateMessage(version);
            if (versionMessage != null) {
                plugin.getLogger().severe(versionMessage);
            }
        }
        return true;
    }
}
