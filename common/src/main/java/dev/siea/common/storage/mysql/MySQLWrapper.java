package dev.siea.common.storage.mysql;

import dev.siea.common.Common;
import dev.siea.common.storage.models.Account;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MySQLWrapper {
    private final String url;
    private final String user;
    private final String psw;
    private final Common p;
    private Connection connection;
    private final long IDLE_TIMEOUT = 1800000; // 1 minute in milliseconds
    private long lastConnectionTime = System.currentTimeMillis();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public MySQLWrapper(Common p) throws SQLException{
        String ip = p.getConfigString("database.ip");
        String name = p.getConfigString("database.name");
        url = "jdbc:mysql://" + ip + "/" + name;
        user = p.getConfigString("database.user");
        psw = p.getConfigString("database.password");
        this.p = p;
        createTables();
        executorService.scheduleWithFixedDelay(() -> {
            long currentTime = System.currentTimeMillis();
            if (connection != null && (currentTime - lastConnectionTime) > IDLE_TIMEOUT) {
                try {
                    destroyConnection();
                    establishConnection();
                } catch (SQLException e) {
                    p.log("Failed to restart MYSQL connection...");
                }
            }
        }, IDLE_TIMEOUT, IDLE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public Connection getConnection() throws SQLException {
        long currentTime = System.currentTimeMillis();
        if (connection != null) {
            return connection;
        }
        establishConnection();
        lastConnectionTime = currentTime;
        return connection;
    }

    public Account findAccountByUUID(String uuid) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM Accounts WHERE uuid = ?");
        statement.setString(1, uuid);

        ResultSet results = statement.executeQuery();

        if (results.next()){
            String discordTag = results.getString("discordTag");
            Account titleData = new Account(discordTag, uuid);
            statement.close();
            return titleData;
        }else{
            statement.close();
            return null;
        }
    }

    public Account findAccountByDiscordID(String id) throws SQLException {
        PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM Accounts WHERE discordTag = ?");
        statement.setString(1, id);

        ResultSet results = statement.executeQuery();

        if (results.next()){
            String uuid = results.getString("uuid");
            Account titleData = new Account(id, uuid);
            statement.close();
            return titleData;
        }else{
            statement.close();
            return null;
        }
    }

    public void createAccount(String UUID, String discordID) throws SQLException{
        PreparedStatement statement = getConnection()
                .prepareStatement("INSERT INTO Accounts (uuid,discordTag) VALUES (?, ?)");
        statement.setString(1, UUID);
        statement.setString(2, discordID);
        statement.executeUpdate();
        statement.close();
    }

    public void deleteAccount(String toString) throws SQLException {
        PreparedStatement statement = getConnection()
                .prepareStatement("DELETE FROM Accounts WHERE uuid = ?");
        statement.setString(1, toString);
        statement.executeUpdate();
        statement.close();
    }

    public void onDisable() throws SQLException{
        destroyConnection();
        executorService.shutdown();
    }

    private void establishConnection ()throws SQLException{
        connection = DriverManager.getConnection(url, user, psw);
    }

    private void createTables() throws SQLException{
        Connection connection = getConnection();
        // CREATE && LOAD Accounts-TABLE
        Statement statementTitleTable = connection.createStatement();
        String sqlPlayerFundDataTable = "CREATE TABLE IF NOT EXISTS Accounts(uuid varchar(36) primary key, discordTag varchar(25))";
        statementTitleTable.execute(sqlPlayerFundDataTable);
        statementTitleTable.close();

        p.log("[DB] Accounts loaded");
    }

    private void destroyConnection() throws SQLException{
        connection.close();
    }
}

