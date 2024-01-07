package dev.siea.discord2fa.discord;

import dev.siea.discord2fa.Discord2FA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.bukkit.plugin.Plugin;

import javax.security.auth.login.LoginException;

public class DiscordBot extends ListenerAdapter {
    private final ShardManager shardManager;
    public DiscordBot(Plugin plugin) throws LoginException {
        String token = plugin.getConfig().getString("Discord.token");
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT).enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.setStatus(OnlineStatus.ONLINE);
        shardManager = builder.build();
        shardManager.addEventListener(new DiscordUtils());
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        DiscordUtils.init();
        Discord2FA.getPlugin().getLogger().info("Discord Bot is ready!");
    }
    public ShardManager getShardManager() {
        return shardManager;
    }
}
