package dev.siea.discord2fa.manager;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.storage.StorageManager;
import dev.siea.discord2fa.storage.models.Account;
import dev.siea.discord2fa.discord.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class VerifyManager implements Listener {
    private static final List<Player> verifyingPlayers = new ArrayList<>();
    private static String verifyDenied;
    private static String verifySuccess;
    private static final String verifyTitle = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("messages.verifyTitle")).replace("&", "§");
    private static List<String> allowedCommands;
    private static final HashMap<Player, Integer> titleCooldown = new HashMap<>();

    public VerifyManager(){
        verifyDenied = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("messages.verifyDenied")).replace("&", "§");
        verifySuccess = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("messages.verifySuccess")).replace("&", "§");
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

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent e){
        if(StorageManager.isLinked(e.getPlayer())){
            verifyingPlayers.add(e.getPlayer());
            sendTitle(e.getPlayer());
            Account account = StorageManager.findAccountByUUID(e.getPlayer().getUniqueId().toString());
            assert account != null;
            DiscordUtils.sendVerify(account, Objects.requireNonNull(e.getPlayer().getAddress()).getAddress().getHostAddress());
        }
    }

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent e){
        verifyingPlayers.remove(e.getPlayer());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public static void onPlayerMove(PlayerMoveEvent e){
        if (!verifyingPlayers.contains(e.getPlayer())) return;
        e.setCancelled(true);
        sendTitle(e.getPlayer());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public static void onCommandEntered(PlayerCommandPreprocessEvent e){
        if (allowedCommands.contains(e.getMessage())) return;
        if (!verifyingPlayers.contains(e.getPlayer())) return;
        e.setCancelled(true);
        sendTitle(e.getPlayer());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public static void onPlayerDropItem(PlayerDropItemEvent e){
        if (!verifyingPlayers.contains(e.getPlayer())) return;
        e.setCancelled(true);
        sendTitle(e.getPlayer());
    }

    @EventHandler  (priority = EventPriority.HIGHEST)
    public static void onPlayerInteract(PlayerInteractEvent e){
        if (!verifyingPlayers.contains(e.getPlayer())) return;
        e.setCancelled(true);
        sendTitle(e.getPlayer());
    }

    public static void verifying(Player player, boolean allowed){
       if (!verifyingPlayers.contains(player)) return;
       if(allowed){
           verifyingPlayers.remove(player);
           player.sendMessage(verifySuccess);
        }else{
           verifyingPlayers.remove(player);
           kickPlayerAsync(player, verifyDenied);
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
        p.sendTitle(verifyTitle,"", 10, 70, 20);
        titleCooldown.put(p, 5);
    }
}
