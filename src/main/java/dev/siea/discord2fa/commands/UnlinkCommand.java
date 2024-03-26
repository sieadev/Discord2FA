package dev.siea.discord2fa.commands;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.manager.LinkManager;
import dev.siea.discord2fa.manager.VerifyManager;
import dev.siea.discord2fa.storage.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class UnlinkCommand  implements CommandExecutor {
    private final String notVerified;
    private final String unlinkSuccess;
    private final String notLinked;

    public UnlinkCommand(){
        Plugin plugin = Discord2FA.getPlugin();
        unlinkSuccess = Objects.requireNonNull(plugin.getConfig().getString("messages.unlinkSuccess")).replace("&", "§");
        notLinked = Objects.requireNonNull(plugin.getConfig().getString("messages.notLinked")).replace("&", "§");
        notVerified = Objects.requireNonNull(plugin.getConfig().getString("messages.verifyTitle")).replace("&", "§");
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        Player player = (Player) sender;
        if (!StorageManager.isLinked(player)){
            player.sendMessage(notLinked);
            return true;
        }
        if (VerifyManager.isVerifying(player)){
            player.sendMessage(notVerified);
            return true;
        }
        LinkManager.unlink(player);
        player.sendMessage(unlinkSuccess);
        return true;
    }
}

