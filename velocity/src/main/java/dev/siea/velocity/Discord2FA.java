package dev.siea.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.siea.common.Common;
import dev.siea.common.CommonException;
import dev.siea.common.discord.DiscordUtils;
import dev.siea.common.messages.Messages;
import dev.siea.common.storage.CommonStorageManager;
import dev.siea.common.util.UpdateChecker;
import dev.siea.velocity.commands.Discord2FACommand;
import dev.siea.velocity.commands.LinkCommand;
import dev.siea.velocity.commands.UnlinkCommand;
import dev.siea.velocity.managers.LinkManager;
import dev.siea.velocity.managers.VerifyManager;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.slf4j.Logger;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import java.nio.file.Path;

@Plugin(
        id = "discord2fa",
        name = "Discord2FA",
        version = "1.5.3",
        description = "2FA with Discord",
        authors = {"sieadev"}
)
public class Discord2FA {
    private static ProxyServer proxy;
    private static Discord2FA discord2FA;
    private static CommandManager commandManager;
    private static Common common;
    private final LinkManager linkManager;
    private final VerifyManager verifyManager;
    private final Logger componentLogger;

    @Inject
    public Discord2FA(ProxyServer proxy, Logger logger, CommandManager commandManager, @DataDirectory Path dataDirectory){
        Discord2FA.proxy = proxy;
        Discord2FA.commandManager = commandManager;
        discord2FA = this;
        componentLogger = logger;
        linkManager = new LinkManager();
        verifyManager = new VerifyManager(proxy,this);

        try {
            common = new Common(linkManager, verifyManager, dataDirectory);
        } catch (CommonException e) {
            logger.error(e.getMessage());
            return;
        }

        proxy.getEventManager().register(this, verifyManager);
        commandManager.register("discord2fa", new Discord2FACommand(this));
        commandManager.register("link", new LinkCommand());
        commandManager.register("unlink", new UnlinkCommand());

        String versionMessage = UpdateChecker.generateUpdateMessage(getProxy().getVersion().getVersion());
        if (versionMessage != null) {
            ComponentLogger.logger().error(versionMessage);
        }
        enableBStats();
    }

    private void enableBStats(){
        //
    }

    public static LinkManager getLinkManager() {
        return discord2FA.linkManager;
    }

    public static VerifyManager getVerifyManager() {
        return discord2FA.verifyManager;
    }

    public static DiscordUtils getDiscordUtils() {
        return common.getDiscordUtils();
    }

    public static CommonStorageManager getStorageManager() {
        return common.getStorageManager();
    }

    public static Messages getMessages(){
        return common.getMessages();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        verifyManager.loadConfig();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event){

    }

    public ProxyServer getProxy(){
        return proxy;
    }

    public Common getCommon() {
        return common;
    }

    public void reload() {
        common.reload();
    }

    public Logger getLogger() {
        return componentLogger;
    }
}
