package dev.siea.common.storage.mysql;


import dev.siea.common.storage.Storage;
import dev.siea.common.storage.models.Account;

import java.sql.SQLException;

public class MySQLStorage implements Storage {
    private final MySQLWrapper mySQLWrapper;

    public MySQLStorage(MySQLWrapper mySQLWrapper) {
        this.mySQLWrapper = mySQLWrapper;
    }

    @Override
    public boolean isLinked(String uuid) {
        try {
            return mySQLWrapper.findAccountByUUID(uuid) != null;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void linkAccount(String uuid, String discordID) {
        try {
            mySQLWrapper.createAccount(uuid, discordID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isLinkedByDiscord(String discordID) {
        try {
            return mySQLWrapper.findAccountByDiscordID(discordID) != null;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void unlinkAccount(String uuid) {
        try {
            mySQLWrapper.deleteAccount(uuid);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Account findAccountByUUID(String uuid) {
        try {
            return mySQLWrapper.findAccountByUUID(uuid);
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public Account findAccountByDiscordID(String discordID) {
        try {
            return mySQLWrapper.findAccountByDiscordID(discordID);
        } catch (SQLException e) {
            return null;
        }
    }
}
