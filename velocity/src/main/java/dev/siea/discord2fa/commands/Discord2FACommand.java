package dev.siea.discord2fa.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.siea.common.util.UpdateChecker;
import dev.siea.discord2fa.Discord2FA;
import net.kyori.adventure.text.Component;

public class Discord2FACommand implements SimpleCommand {

    private final String version;
    private final Discord2FA plugin;

    public Discord2FACommand(Discord2FA plugin) {
        version = plugin.getProxy().getVersion().getVersion();
        this.plugin = plugin;
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            long startTime = System.currentTimeMillis();
            plugin.reload();
            long duration = System.currentTimeMillis() - startTime;
            if (sender instanceof Player) {
                sender.sendMessage(Component.text("§eReloaded Discord2FA in §6" + duration + "§e milliseconds!"));
            } else {
                sender.sendMessage(Component.text("Reloaded Discord2FA in " + duration + " milliseconds!"));
            }
            return;
        }
        if (sender instanceof Player player){
            player.sendMessage(Component.text(UpdateChecker.generateUpdateMessageColored(version)));
        }
        else {
            String versionMessage = UpdateChecker.generateUpdateMessage(version);
            if (versionMessage != null) {
                plugin.getLogger().info(versionMessage);
            }
        }
    }
}
