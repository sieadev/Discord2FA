package dev.siea.discord2fa.commands;

import dev.siea.discord2fa.manager.LinkManager;
import dev.siea.discord2fa.message.Messages;
import dev.siea.discord2fa.storage.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LinkCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        Player player = (Player) sender;
        if (StorageManager.isLinked(player)){
            player.sendMessage(Messages.get("alreadyLinked"));
            return true;
        }
        if (args.length < 1){
            player.sendMessage(Messages.get("noCode"));
            return true;
        }
        String code = args[0];
        LinkManager.tryLink(player, code);
        return true;
    }
}
