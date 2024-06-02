package dev.siea.spigot.commands;

import dev.siea.spigot.Discord2FA;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnlinkCommand  implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args){
        if (!(sender instanceof Player player)){
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        if (!Discord2FA.getStorageManager().isLinked(player.getUniqueId().toString())){
            player.sendMessage(Discord2FA.getMessages().get("notLinked"));
            return true;
        }
        if (Discord2FA.getVerifyManager().isVerifying(player)){
            player.sendMessage(Discord2FA.getMessages().get("notVerified"));
            return true;
        }
        Discord2FA.getLinkManager().unlink(player);
        player.sendMessage(Discord2FA.getMessages().get("unlinkSuccess"));
        return true;
    }
}

