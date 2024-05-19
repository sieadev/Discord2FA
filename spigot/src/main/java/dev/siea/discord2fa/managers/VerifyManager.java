package dev.siea.discord2fa.managers;

import dev.siea.common.base.BaseVerifyManager;
import dev.siea.common.storage.models.Account;
import dev.siea.discord2fa.Discord2FA;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.*;

public class VerifyManager implements Listener, BaseVerifyManager {
    private final List<Player> verifyingPlayers = new ArrayList<>();
    private final List<Player> forcedPlayers = new ArrayList<>();
    private final List<String> allowedCommands;
    private final HashMap<Player, Integer> titleCooldown = new HashMap<>();
    private final boolean forceLink = Discord2FA.getPlugin().getConfig().getBoolean("force-link");

    public VerifyManager(){
        allowedCommands = Discord2FA.getPlugin().getConfig().getStringList("allowedCommands");
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Discord2FA.getPlugin(), () -> {
            for (Player p : titleCooldown.keySet()) {
                if (titleCooldown.get(p) == 0) {
                    titleCooldown.remove(p);
                    continue;
                }
                titleCooldown.put(p, titleCooldown.get(p) - 1);
            }
        }, 0, 20);
    }

    public void linked(Player player){
        forcedPlayers.remove(player);
        Discord2FA.getStorageManager().updateIPAddress(player.getUniqueId().toString(), Objects.requireNonNull(player.getAddress()).getAddress().toString());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        if(Discord2FA.getStorageManager().isLinked(e.getPlayer().getUniqueId().toString())){
            if (Discord2FA.getStorageManager().isRemembered(e.getPlayer().getUniqueId().toString(), Objects.requireNonNull(e.getPlayer().getAddress()).getAddress().toString())){
                return;
            }
            verifyingPlayers.add(e.getPlayer());
            sendTitle(e.getPlayer());
            Account account = Discord2FA.getStorageManager().findAccountByUUID(e.getPlayer().getUniqueId().toString());
            assert account != null;
            Discord2FA.getDiscordUtils().sendVerify(account, Objects.requireNonNull(e.getPlayer().getAddress()).getAddress().getHostAddress());
        } else if (forceLink) {
            forcedPlayers.add(e.getPlayer());
            e.getPlayer().sendMessage(Discord2FA.getMessages().get("forceLink"));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        verifyingPlayers.remove(e.getPlayer());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent e){
        Player player = e.getPlayer();
        if (verifyingPlayers.contains(player)) {
            e.setCancelled(true);
            sendTitle(player);
        }
        else if (forcedPlayers.contains(player)){
            e.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent e){
        Player player = (Player) e.getPlayer();
        if (verifyingPlayers.contains(player)) {
            e.setCancelled(true);
            sendTitle(player);
        }
        else if (forcedPlayers.contains(player)){
            e.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onCommandEntered(PlayerCommandPreprocessEvent e){
        if (allowedCommands.contains(e.getMessage().split(" ")[0])) return;
        Player player = e.getPlayer();
        if (verifyingPlayers.contains(player)) {
            e.setCancelled(true);
            sendTitle(player);
        }
        else if (forcedPlayers.contains(player)){
            e.setCancelled(true);
            sendTitle(player);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent e){
        Player player = e.getPlayer();
        if (verifyingPlayers.contains(player)) {
            e.setCancelled(true);
            sendTitle(player);
        }
        else if (forcedPlayers.contains(player)){
            e.setCancelled(true);
        }
    }

    @EventHandler  (priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent e){
        Player player = e.getPlayer();
        if (verifyingPlayers.contains(player)) {
            e.setCancelled(true);
            sendTitle(player);
        }
        else if (forcedPlayers.contains(player)){
            e.setCancelled(true);
        }
    }

    @Override
    public void verifying(String p, boolean allowed){
        Player player = Discord2FA.getPlugin().getServer().getPlayer(UUID.fromString(p));
        assert player != null;
       if (!verifyingPlayers.contains(player)) return;
       if(allowed){
           verifyingPlayers.remove(player);
           player.sendMessage(Discord2FA.getMessages().get("verifySuccess"));
           Discord2FA.getStorageManager().updateIPAddress(player.getUniqueId().toString(), Objects.requireNonNull(player.getAddress()).getAddress().toString());
        }else{
           verifyingPlayers.remove(player);
           kickPlayerAsync(player, Discord2FA.getMessages().get("verifyDenied"));
       }
    }

    private void kickPlayerAsync(Player player, String kickMessage) {
        Bukkit.getScheduler().runTask(Discord2FA.getPlugin(), () -> player.kickPlayer(kickMessage));
    }

    public boolean isVerifying(Player player){
        return verifyingPlayers.contains(player);
    }

    private void sendTitle(Player p){
        if (titleCooldown.containsKey(p)) return;
        p.sendTitle(Discord2FA.getMessages().get("verifyTitle"),"", 10, 70, 20);
        titleCooldown.put(p, 5);
    }
}
