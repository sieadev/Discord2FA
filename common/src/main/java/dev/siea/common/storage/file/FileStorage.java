package dev.siea.common.storage.file;

import dev.siea.common.storage.Storage;
import dev.siea.common.storage.models.Account;

import java.nio.file.Path;

public class FileStorage implements Storage {
    private final FileWrapper fileWrapper;
    public FileStorage(Path dir) {
        this.fileWrapper = new FileWrapper(dir);
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
