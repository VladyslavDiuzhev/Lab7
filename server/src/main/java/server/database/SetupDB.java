package server.database;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;

public abstract class SetupDB {
    private static Connection connection;

    public static Connection getConnection() {
        return connection;
    }

    public static boolean createConnection() {
        Connection c;
        try {
            Class.forName("org.postgresql.Driver");
            Properties info = new Properties();
//            info.load(new FileInputStream("db.cfg"));
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/studs", "s336225", "eoc653");

//            System.out.println("Успешное соединение с БД.");
            //          info.load(new FileInputStream("src/main/java/info_helios.cfg"));
//            c = DriverManager.getConnection("jdbc:postgresql://pg:5432/studs", "s", "");

            connection = c;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void createTables() {
        
        System.out.println("SQL tables are ready to use\n");
    }
}
