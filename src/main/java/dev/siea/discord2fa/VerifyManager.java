package dev.siea.discord2fa;

import dev.siea.discord2fa.database.AccountUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VerifyManager implements Listener {
    private static List<Player> verifying = new ArrayList<>();

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent e){
        if(AccountUtil.isLinked(e.getPlayer())){
            verifying.add(e.getPlayer());
        }
    }

    private static void
}
