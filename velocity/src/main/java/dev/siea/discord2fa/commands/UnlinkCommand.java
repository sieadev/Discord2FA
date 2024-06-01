package dev.siea.discord2fa.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.siea.discord2fa.Discord2FA;
import net.kyori.adventure.text.Component;

public class UnlinkCommand implements SimpleCommand {

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource sender = invocation.source();

        if (!(sender instanceof Player player)){
            sender.sendMessage(Component.text("You must be a player to use this command!"));
            return;
        }
        if (!Discord2FA.getStorageManager().isLinked(player.getUniqueId().toString())){
            player.sendMessage(Component.text(Discord2FA.getMessages().get("notLinked")));
            return;
        }
        if (Discord2FA.getVerifyManager().isVerifying(player)){
            player.sendMessage(Component.text(Discord2FA.getMessages().get("notVerified")));
            return;
        }
        Discord2FA.getLinkManager().unlink(player);
        player.sendMessage(Component.text(Discord2FA.getMessages().get("unlinkSuccess")));
    }
}
