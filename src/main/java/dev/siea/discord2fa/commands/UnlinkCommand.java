package dev.siea.discord2fa.commands;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.manager.LinkManager;
import dev.siea.discord2fa.manager.VerifyManager;
import dev.siea.discord2fa.message.Messages;
import dev.siea.discord2fa.storage.StorageManager;
import dev.siea.discord2fa.util.ConfigUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class UnlinkCommand  implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        Player player = (Player) sender;
        if (!StorageManager.isLinked(player)){
            player.sendMessage(Messages.get("notLinked"));
            return true;
        }
        if (VerifyManager.isVerifying(player)){
            player.sendMessage(Messages.get("notVerified"));
            return true;
        }
        LinkManager.unlink(player);
        player.sendMessage(Messages.get("unlinkSuccess"));
        return true;
    }
}

