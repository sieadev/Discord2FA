package dev.siea.discord2fa.commands;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.util.UpdateChecker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Discord2FACommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args){
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            long startTime = System.currentTimeMillis();
            Discord2FA.reload();
            long duration = System.currentTimeMillis() - startTime;
            if (sender instanceof Player) {
                sender.sendMessage("§eReloaded Discord2FA in §6" + duration + "§e milliseconds!");
            } else {
                sender.sendMessage("Reloaded Discord2FA in " + duration + " milliseconds!");
            }
            return true;
        }
        if (sender instanceof Player){
            new UpdateChecker(Discord2FA.getPlugin(), (Player) sender);
            return true;
        }
        else {
            new UpdateChecker(Discord2FA.getPlugin());
        }
        return true;
    }
}
