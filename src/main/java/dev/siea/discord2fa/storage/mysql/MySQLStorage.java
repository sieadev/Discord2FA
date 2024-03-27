package dev.siea.discord2fa.storage.mysql;

import dev.siea.discord2fa.storage.Storage;
import dev.siea.discord2fa.storage.models.Account;

import java.sql.SQLException;

public class MySQLStorage implements Storage {
    @Override
    public boolean isLinked(String uuid) {
        try {
            return MySQLWrapper.findAccountByUUID(uuid) != null;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void linkAccount(String uuid, String discordID) {
        try {
            MySQLWrapper.createAccount(uuid, discordID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isLinkedByDiscord(String discordID) {
        try {
            return MySQLWrapper.findAccountByDiscordID(discordID) != null;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void unlinkAccount(String uuid) {
        try {
            MySQLWrapper.deleteAccount(uuid);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Account findAccountByUUID(String uuid) {
        try {
            return MySQLWrapper.findAccountByUUID(uuid);
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public Account findAccountByDiscordID(String discordID) {
        try {
            return MySQLWrapper.findAccountByDiscordID(discordID);
        } catch (SQLException e) {
            return null;
        }
    }
}
