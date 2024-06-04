package dev.siea.velocity.managers;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.siea.common.base.BaseVerifyManager;
import dev.siea.common.storage.models.Account;
import dev.siea.velocity.Discord2FA;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class VerifyManager implements BaseVerifyManager {
    private final List<Player> verifyingPlayers = new ArrayList<>();
    private final List<Player> forcedPlayers = new ArrayList<>();
    private List<String> allowedCommands;
    private final HashMap<Player, Integer> titleCooldown = new HashMap<>();
    private boolean forceLink;
    private final ProxyServer server;
    private final Discord2FA plugin;

    public VerifyManager(ProxyServer server, Discord2FA plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    public void loadConfig(){
        allowedCommands = plugin.getCommon().getConfig().getConfig().getStringList("allowedCommands");
        forceLink = plugin.getCommon().getConfig().getConfig().getBoolean("force-link");
        server.getScheduler().buildTask(plugin, this::updateTitleCooldowns).repeat(1L, TimeUnit.SECONDS).schedule();
    }

    private void updateTitleCooldowns() {
        for (Iterator<Map.Entry<Player, Integer>> it = titleCooldown.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Player, Integer> entry = it.next();
            int newCooldown = entry.getValue() - 1;
            if (newCooldown <= 0) {
                it.remove();
            } else {
                entry.setValue(newCooldown);
            }
        }
    }

    public void linked(Player player) {
        forcedPlayers.remove(player);
        Discord2FA.getStorageManager().updateIPAddress(player.getUniqueId().toString(), Objects.requireNonNull(player.getRemoteAddress()).getAddress().toString());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        server.getScheduler().buildTask(plugin, this::updateTitleCooldowns).repeat(1L, TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void onPlayerJoin(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        if (Discord2FA.getStorageManager().isLinked(player.getUniqueId().toString())) {
            if (Discord2FA.getStorageManager().isRemembered(player.getUniqueId().toString(), Objects.requireNonNull(player.getRemoteAddress()).getAddress().toString())) {
                return;
            }
            verifyingPlayers.add(player);
            sendTitle(player);
            Account account = Discord2FA.getStorageManager().findAccountByUUID(player.getUniqueId().toString());
            assert account != null;
            Discord2FA.getDiscordUtils().sendVerify(account, Objects.requireNonNull(player.getRemoteAddress()).getAddress().getHostAddress());
        } else if (forceLink) {
            forcedPlayers.add(player);
            player.sendMessage(Component.text(Discord2FA.getMessages().get("forceLink")));
        }
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        Player player = event.getPlayer();
        verifyingPlayers.remove(player);
        forcedPlayers.remove(player);
    }

    @Subscribe
    public void onPlayerCommandPreprocess(CommandExecuteEvent event) {
        CommandSource source = event.getCommandSource();
        if (source instanceof Player player) {
            if (verifyingPlayers.contains(player) || forcedPlayers.contains(player)) {
                if (!allowedCommands.contains(event.getCommand().split(" ")[0])) {
                    event.setResult(CommandExecuteEvent.CommandResult.denied());
                    sendTitle(player);
                }
            }
        }
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (verifyingPlayers.contains(player) || forcedPlayers.contains(player)) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            sendTitle(player);
        }
    }

    @Override
    public void verifying(String p, boolean allowed) {
        Player player = server.getPlayer(UUID.fromString(p)).orElse(null);
        if (player == null || !verifyingPlayers.contains(player)) {
            return;
        }
        if (allowed) {
            verifyingPlayers.remove(player);
            player.sendMessage(Component.text(Discord2FA.getMessages().get("verifySuccess")));
            Discord2FA.getStorageManager().updateIPAddress(player.getUniqueId().toString(), Objects.requireNonNull(player.getRemoteAddress()).getAddress().toString());
        } else {
            verifyingPlayers.remove(player);
            kickPlayerAsync(player, Discord2FA.getMessages().get("verifyDenied"));
        }
    }

    private void kickPlayerAsync(Player player, String kickMessage) {
        server.getScheduler().buildTask(plugin, () -> player.disconnect(Component.text(kickMessage))).schedule();
    }

    public boolean isVerifying(Player player) {
        return verifyingPlayers.contains(player);
    }

    private void sendTitle(Player player) {
        if (titleCooldown.containsKey(player)) {
            return;
        }
        player.showTitle(Title.title(
                Component.text(Discord2FA.getMessages().get("verifyTitle")),
                Component.empty(),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1500), Duration.ofMillis(500))
        ));
        titleCooldown.put(player, 5);
    }
}
