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
        int port = server.run(8081);  // Changed from 8080 to avoid macOS Sonoma WebSocket issue

        System.out.println("========================================");
        System.out.println("240 Chess Server");
        System.out.println("Server running on port: " + port);
        System.out.println("WebSocket endpoint: ws://localhost:" + port + "/ws");
        System.out.println("========================================");
    }
}
