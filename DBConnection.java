import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection 
{
    public static Connection getConnection() throws SQLException, IOException, ClassNotFoundException 
    {
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream("config.properties");
        props.load(fis);

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.pass");

        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(url, user, password);
    }
}
