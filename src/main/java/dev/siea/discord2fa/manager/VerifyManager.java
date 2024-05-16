package dev.siea.discord2fa.manager;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.message.Messages;
import dev.siea.discord2fa.storage.StorageManager;
import dev.siea.discord2fa.storage.models.Account;
import dev.siea.discord2fa.discord.DiscordUtils;
import dev.siea.discord2fa.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class VerifyManager implements Listener {
    private static final List<Player> verifyingPlayers = new ArrayList<>();
    private static final List<Player> forcedPlayers = new ArrayList<>();
    private static List<String> allowedCommands;
    private static final HashMap<Player, Integer> titleCooldown = new HashMap<>();
    private static final boolean forceLink = Discord2FA.getPlugin().getConfig().getBoolean("force-link");

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

    public static void linked(Player player){
        forcedPlayers.remove(player);
        StorageManager.updateIPAddress(player);
    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent e){
        if(StorageManager.isLinked(e.getPlayer())){
            if (StorageManager.isRemembered(e.getPlayer())){
                return;
            }
            verifyingPlayers.add(e.getPlayer());
            sendTitle(e.getPlayer());
            Account account = StorageManager.findAccountByUUID(e.getPlayer().getUniqueId().toString());
            assert account != null;
            DiscordUtils.sendVerify(account, Objects.requireNonNull(e.getPlayer().getAddress()).getAddress().getHostAddress());
        } else if (forceLink) {
            forcedPlayers.add(e.getPlayer());
            e.getPlayer().sendMessage(Messages.get("forceLink"));
        }
    }

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent e){
        verifyingPlayers.remove(e.getPlayer());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public static void onPlayerMove(PlayerMoveEvent e){
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
    public static void onInventoryOpen(InventoryOpenEvent e){
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
    public static void onCommandEntered(PlayerCommandPreprocessEvent e){
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
    public static void onPlayerDropItem(PlayerDropItemEvent e){
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
    public static void onPlayerInteract(PlayerInteractEvent e){
        Player player = e.getPlayer();
        if (verifyingPlayers.contains(player)) {
            e.setCancelled(true);
            sendTitle(player);
        }
        else if (forcedPlayers.contains(player)){
            e.setCancelled(true);
        }
    }

    public static void verifying(Player player, boolean allowed){
       if (!verifyingPlayers.contains(player)) return;
       if(allowed){
           verifyingPlayers.remove(player);
           player.sendMessage(Messages.get("verifySuccess"));
           StorageManager.updateIPAddress(player);
        }else{
           verifyingPlayers.remove(player);
           kickPlayerAsync(player, Messages.get("verifyDenied"));
       }
    }

    private static void kickPlayerAsync(Player player, String kickMessage) {
        Bukkit.getScheduler().runTask(Discord2FA.getPlugin(), () -> player.kickPlayer(kickMessage));
    }

    public static boolean isVerifying(Player player){
        return verifyingPlayers.contains(player);
    }

    private static void sendTitle(Player p){
        if (titleCooldown.containsKey(p)) return;
        p.sendTitle(Messages.get("verifyTitle"),"", 10, 70, 20);
        titleCooldown.put(p, 5);
    }
}
