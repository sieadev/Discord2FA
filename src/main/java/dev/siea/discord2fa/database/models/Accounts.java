package dev.siea.discord2fa.database.models;

public class Accounts {
    private final String discordID;
    private final String minecraftUUID;
    public Accounts(String discordID, String minecraftUUID) {
        this.discordID = discordID;
        this.minecraftUUID = minecraftUUID;
    }

    public String getDiscordID() {
        return discordID;
    }

    public String getMinecraftUUID() {
        return minecraftUUID;
    }
}
