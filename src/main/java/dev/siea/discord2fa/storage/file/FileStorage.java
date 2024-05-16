package dev.siea.discord2fa.storage.file;

import dev.siea.discord2fa.storage.Storage;
import dev.siea.discord2fa.storage.models.Account;

public class FileStorage implements Storage {
    private final FileWrapper fileWrapper;
    public FileStorage() {
        this.fileWrapper = new FileWrapper();
    }
    @Override
    public boolean isLinked(String uuid) {
        return fileWrapper.findAccountByUUID(uuid) != null;
    }

    @Override
    public void linkAccount(String uuid, String discordID) {
        fileWrapper.createAccount(uuid, discordID);
    }

    @Override
    public boolean isLinkedByDiscord(String discordID) {
        return fileWrapper.findAccountByDiscordID(discordID) != null;
    }

    @Override
    public void unlinkAccount(String uuid) {
        fileWrapper.deleteAccount(uuid);
    }

    @Override
    public Account findAccountByUUID(String uuid) {
        return fileWrapper.findAccountByUUID(uuid);
    }

    @Override
    public Account findAccountByDiscordID(String discordID) {
        return fileWrapper.findAccountByDiscordID(discordID);
    }
}
