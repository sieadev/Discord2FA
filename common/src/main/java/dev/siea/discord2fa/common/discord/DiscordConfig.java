package dev.siea.discord2fa.common.discord;

import dev.siea.discord2fa.common.config.ConfigAdapter;

/**
 * Discord bot settings from config. Expected keys: discord.token, discord.guild, discord.channel.
 */
public class DiscordConfig {
    private final String token;
    private final long guildId;
    private final long channelId;

    public DiscordConfig(ConfigAdapter configAdapter) {
        this.token = trimToNull(configAdapter.getString("discord.token"));
        this.guildId = parseLong(configAdapter.getString("discord.guild"), 0L);
        this.channelId = parseLong(configAdapter.getString("discord.channel"), 0L);
    }

    public String getToken() {
        return token;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public boolean isConfigured() {
        return token != null && !token.isBlank() && guildId != 0 && channelId != 0;
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    private static long parseLong(String s, long defaultValue) {
        if (s == null || s.isBlank()) return defaultValue;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
