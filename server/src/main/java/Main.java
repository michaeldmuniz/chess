import dataaccess.MySQLDataAccess;
import dataaccess.DataAccessException;
import server.Server;

public class Main {
    public static void main(String[] args) {
        try {
            MySQLDataAccess dao = new MySQLDataAccess();
        } catch (DataAccessException e) {
            System.out.println(" Database connection failed: " + e.getMessage());
            return;
        }

        Server server = new Server();
        server.run(8080);

        System.out.println("240 Chess Server");
    }
}
