package dev.siea.discord2fa.manager;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.database.AccountUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VerifyManager implements Listener {
    private static List<Player> verifying = new ArrayList<>();
    private static String verifyDenied;
    private static String verifySuccess;

    public VerifyManager(){
        verifyDenied = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("verifyDenied")).replace("&", "§");
        verifySuccess = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("verifySuccess")).replace("&", "§");
    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent e){
        if(AccountUtil.isLinked(e.getPlayer())){
            verifying.add(e.getPlayer());
        }
    }

    public static void verifying(Player player, boolean allowed){
       if(allowed){
           verifying.remove(player);
           player.sendMessage(verifySuccess);
        }else{
           verifying.remove(player);
           player.kickPlayer(verifyDenied);
       }
    }


}
