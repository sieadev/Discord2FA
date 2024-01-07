package dev.siea.discord2fa.commands;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.database.AccountUtil;
import dev.siea.discord2fa.manager.LinkManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class LinkCommand implements CommandExecutor {
    private String alreadyLinked;
    private String noCode;

    public LinkCommand(){
        Plugin plugin = Discord2FA.getPlugin();
        alreadyLinked = Objects.requireNonNull(plugin.getConfig().getString("messages.alreadyLinked")).replace("&", "§");
        noCode = Objects.requireNonNull(plugin.getConfig().getString("messages.noCode")).replace("&", "§");

    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        Player player = (Player) sender;
        if (AccountUtil.isLinked(player)){
            player.sendMessage(alreadyLinked);
            return true;
        }
        if (args.length < 1){
            player.sendMessage(noCode);
            return true;
        }
        String code = args[0];
        LinkManager.tryLink(player, code);
        return true;
    }
}
