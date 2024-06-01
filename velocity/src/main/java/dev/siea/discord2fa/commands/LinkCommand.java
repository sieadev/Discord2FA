package dev.siea.discord2fa.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.siea.discord2fa.Discord2FA;
import net.kyori.adventure.text.Component;

public class LinkCommand implements SimpleCommand {

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (!(sender instanceof Player player)){
            sender.sendMessage(Component.text("You must be a player to use this command!"));
            return;
        }
        if (Discord2FA.getStorageManager().isLinked(player.getUniqueId().toString())){
            player.sendMessage(Component.text(Discord2FA.getMessages().get("alreadyLinked")));
            return;
        }
        if (args.length < 1){
            player.sendMessage(Component.text(Discord2FA.getMessages().get("noCode")));
            return;
        }
        String code = args[0];
        Discord2FA.getLinkManager().tryLink(player, code);
    }
}
