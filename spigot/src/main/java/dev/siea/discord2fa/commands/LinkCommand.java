package dev.siea.discord2fa.commands;

import dev.siea.discord2fa.Discord2FA;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LinkCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args){
        if (!(sender instanceof Player player)){
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        if (Discord2FA.getStorageManager().isLinked(player.getUniqueId().toString())){
            player.sendMessage(Discord2FA.getMessages().get("alreadyLinked"));
            return true;
        }
        if (args.length < 1){
            player.sendMessage(Discord2FA.getMessages().get("noCode"));
            return true;
        }
        String code = args[0];
        Discord2FA.getLinkManager().tryLink(player, code);
        return true;
    }
}
