package dev.siea.discord2fa;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.siea.common.util.UpdateChecker;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.slf4j.Logger;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import java.nio.file.Path;

@Plugin(
        id = "discord2fa",
        name = "Discord2FA",
        version = "1.5.3",
        description = "Proxy Plugin for Vitacraft",
        authors = {"Vitacraft"}
)
public class Discord2FA {
    private ProxyServer proxy;
    private Discord2FA discord2FA;
    private CommandManager commandManager;
    private YamlDocument config;

    @Inject
    public Discord2FA(ProxyServer proxy, Logger logger, CommandManager commandManager, @DataDirectory Path dataDirectory){
        this.proxy = proxy;
        this.commandManager = commandManager;
        discord2FA = this;


        String versionMessage = UpdateChecker.generateUpdateMessage(getProxy().getVersion().getVersion());
        if (versionMessage != null) {
            ComponentLogger.logger().error(versionMessage);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event){

    }

    public ProxyServer getProxy(){
        return proxy;
    }
}
