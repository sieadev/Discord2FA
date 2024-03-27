package dev.siea.discord2fa.storage.mysql;

import dev.siea.discord2fa.storage.models.Account;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.bukkit.Bukkit.getServer;

public class MySQLWrapper {
    private static String url;
    private static String user;
    private static String psw;
    private static Connection connection;
    private static final long IDLE_TIMEOUT = 1800000; // 1 minute in milliseconds
    private static long lastConnectionTime = System.currentTimeMillis();
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static Connection getConnection() throws SQLException {
        long currentTime = System.currentTimeMillis();
        if (connection != null) {
            return connection;
        }
        establishConnection();
        lastConnectionTime = currentTime;
        return connection;
    }

    public static Account findAccountByUUID(String uuid) throws SQLException{
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

    public static Account findAccountByDiscordID(String id) throws SQLException {
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

    public static void createAccount(String UUID, String discordID) throws SQLException{
        PreparedStatement statement = getConnection()
                .prepareStatement("INSERT INTO Accounts (uuid,discordTag) VALUES (?, ?)");
        statement.setString(1, UUID);
        statement.setString(2, discordID);
        statement.executeUpdate();
        statement.close();
    }

    public static void deleteAccount(String toString) throws SQLException {
        PreparedStatement statement = getConnection()
                .prepareStatement("DELETE FROM Accounts WHERE uuid = ?");
        statement.setString(1, toString);
        statement.executeUpdate();
        statement.close();
    }

    public static void onEnable(Plugin p) throws SQLException{
        String ip = p.getConfig().getString("database.ip");
        String name = p.getConfig().getString("database.name");
        url = "jdbc:mysql://" + ip + "/" + name;
        user = p.getConfig().getString("database.user");
        psw = p.getConfig().getString("database.password");
        createTables();
        executorService.scheduleWithFixedDelay(() -> {
            long currentTime = System.currentTimeMillis();
            if (connection != null && (currentTime - lastConnectionTime) > IDLE_TIMEOUT) {
                try {
                    destroyConnection();
                    establishConnection();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, IDLE_TIMEOUT, IDLE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static void onDisable() throws SQLException{
        destroyConnection();
        executorService.shutdown();
    }

    private static void establishConnection ()throws SQLException{
        connection = DriverManager.getConnection(url, user, psw);
    }

    private static void createTables() throws SQLException{
        Connection connection = getConnection();
        // CREATE && LOAD Accounts-TABLE
        Statement statementTitleTable = connection.createStatement();
        String sqlPlayerFundDataTable = "CREATE TABLE IF NOT EXISTS Accounts(uuid varchar(36) primary key, discordTag varchar(25))";
        statementTitleTable.execute(sqlPlayerFundDataTable);
        statementTitleTable.close();

        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[DB] Accounts loaded");
    }

    private static void destroyConnection() throws SQLException{
        connection.close();
    }
}

