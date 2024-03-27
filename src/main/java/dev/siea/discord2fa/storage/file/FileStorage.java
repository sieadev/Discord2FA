package dev.siea.discord2fa.storage.file;

import dev.siea.discord2fa.storage.Storage;
import dev.siea.discord2fa.storage.models.Account;

public class FileStorage implements Storage {
    private final FileUtil fileUtil;
    public FileStorage() {
        this.fileUtil = new FileUtil();
    }
    @Override
    public boolean isLinked(String uuid) {
        return fileUtil.findAccountByUUID(uuid) != null;
    }

    @Override
    public void linkAccount(String uuid, String discordID) {
        fileUtil.createAccount(uuid, discordID);
    }

    @Override
    public boolean isLinkedByDiscord(String discordID) {
        return fileUtil.findAccountByDiscordID(discordID) != null;
    }

    @Override
    public void unlinkAccount(String uuid) {
        fileUtil.deleteAccount(uuid);
    }

    @Override
    public Account findAccountByUUID(String uuid) {
        return fileUtil.findAccountByUUID(uuid);
    }

    @Override
    public Account findAccountByDiscordID(String discordID) {
        return fileUtil.findAccountByDiscordID(discordID);
    }
}
