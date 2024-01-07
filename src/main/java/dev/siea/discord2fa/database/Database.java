package dev.siea.discord2fa.database;

import dev.siea.discord2fa.database.models.Account;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.bukkit.Bukkit.getServer;

public class Database {
    private static String url;
    private static String user;
    private static String psw;
    private static String name;
    private static Connection connection;
    private static final long IDLE_TIMEOUT = 1800000; // 1 minute in milliseconds
    private static long lastConnectionTime = System.currentTimeMillis();
    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

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
        PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM PlayerFundData WHERE uuid = ?");
        statement.setString(1, uuid);

        ResultSet results = statement.executeQuery();

        if (results.next()){
            int unlocked = results.getInt("amount");
            PlayerFundData titleData = new PlayerFundData(uuid, unlocked);
            statement.close();
            return titleData;
        }else{
            statement.close();
            PlayerFundData afa = new PlayerFundData(uuid, 0);
            createPlayerFundData(afa);
            return afa;
        }
    }

    public static void createAccount(Account data) throws SQLException{
        PreparedStatement statement = getConnection()
                .prepareStatement("INSERT INTO PlayerFundData (uuid,amount) VALUES (?, ?)");
        statement.setString(1, data.getPlayer().getUniqueId().toString());
        statement.setInt(2, data.getAmount());
        statement.executeUpdate();
        statement.close();
    }

    public static void updateAccountByUUID(Account data) throws SQLException{
        PreparedStatement statement = getConnection()
                .prepareStatement("UPDATE PlayerFundData SET amount = ? WHERE uuid = ?");
        statement.setInt(1, data.getAmount());
        statement.setString(2, data.getPlayer().getUniqueId().toString());
        statement.executeUpdate();
        statement.close();
    }

    public static void onEnable(Plugin p) throws SQLException{
        String ip = p.getConfig().getString("ip");
        name = p.getConfig().getString("name");
        url = "jdbc:mysql://" + ip + "/" + name;
        user = p.getConfig().getString("user");
        psw = p.getConfig().getString("password");
        createTables();
        executorService.scheduleWithFixedDelay(() -> {
            long currentTime = System.currentTimeMillis();
            if (connection != null && (currentTime - lastConnectionTime) > IDLE_TIMEOUT) {
                try {
                    destroyConnection();
                    getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[DB] Connection to the Database is being stopped.");
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
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[DB] Accounts connected to Database");
    }

    private static void createTables() throws SQLException{
        Connection connection = getConnection();
        // CREATE && LOAD Accounts-TABLE
        Statement statementTitleTable = connection.createStatement();
        String sqlPlayerFundDataTable = "CREATE TABLE IF NOT EXISTS Accounts(uuid varchar(36) primary key, amount int(255))";
        statementTitleTable.execute(sqlPlayerFundDataTable);
        statementTitleTable.close();
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[DB] Accounts table was loaded successfully");
    }

    private static void destroyConnection() throws SQLException{
        connection.close();
    }
}

