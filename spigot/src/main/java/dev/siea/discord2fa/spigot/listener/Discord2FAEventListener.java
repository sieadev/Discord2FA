package dev.siea.discord2fa.spigot.listener;

import dev.siea.discord2fa.common.event.EventType;
import dev.siea.discord2fa.gameserver.server.GameServer;
import dev.siea.discord2fa.spigot.player.SpigotPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Discord2FAEventListener implements Listener {

    private final GameServer server;
    /** Login wrappers keyed by platform player handle so quit can match the correct session. */
    private final Map<Player, SpigotPlayer> wrappersByHandle = new ConcurrentHashMap<>();

    public Discord2FAEventListener(GameServer server) {
        this.server = server;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player handle = event.getPlayer();
        SpigotPlayer player = new SpigotPlayer(handle);
        wrappersByHandle.put(handle, player);

        boolean forceVerify = isSessionTakeover(handle);
        server.handlePlayerJoin(player, forceVerify);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player handle = event.getPlayer();
        SpigotPlayer wrapper = wrappersByHandle.remove(handle);
        if (wrapper != null) {
            server.endPlayerSession(handle.getUniqueId(), wrapper);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!server.onEvent(event.getPlayer().getUniqueId(), EventType.BLOCK_BREAK)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!server.onEvent(event.getPlayer().getUniqueId(), EventType.BLOCK_PLACE)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        if (!server.onEvent(event.getPlayer().getUniqueId(), EventType.MOVE)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!server.onEvent(event.getPlayer().getUniqueId(), EventType.CHAT)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!server.onEvent(event.getPlayer().getUniqueId(), EventType.DROP)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!server.onEvent(((Player) event.getWhoClicked()).getUniqueId(), EventType.INVENTORY)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!server.onEvent(((Player) event.getPlayer()).getUniqueId(), EventType.INVENTORY)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        String label = parseCommandLabel(event.getMessage());
        if (!server.onCommand(player.getUniqueId(), label)) {
            event.setCancelled(true);
            server.getCommandDeniedMessage(player.getUniqueId(), label)
                    .thenAccept(msg -> { if (msg != null) player.sendMessage(msg); });
        }
    }

    private boolean isSessionTakeover(Player newPlayer) {
        UUID uuid = newPlayer.getUniqueId();
        for (Map.Entry<Player, SpigotPlayer> entry : wrappersByHandle.entrySet()) {
            if (entry.getKey().getUniqueId().equals(uuid) && entry.getKey() != newPlayer) {
                return true;
            }
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(uuid) && online != newPlayer) {
                return true;
            }
        }
        return false;
    }

    private static String parseCommandLabel(String message) {
        if (message == null || message.isEmpty()) return "";
        String trimmed = message.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }
}
