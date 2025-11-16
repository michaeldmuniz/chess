package client.ui;

import client.ServerFacade;
import java.io.IOException;
import java.util.Scanner;

public class PostloginUI {

    private final ServerFacade server;
    private final Scanner scanner;

    public PostloginUI(ServerFacade server, Scanner scanner) {
        this.server = server;
        this.scanner = scanner;
    }

    public PostloginResult runOnce(String authToken) {
        PostloginResult result = new PostloginResult();

        System.out.print("[LOGGED_IN] >>> ");
        String line = scanner.nextLine().trim();

        if (line.isEmpty()) {
            return result;
        }

        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "help":
                printHelp();
                return result;

            case "logout":
                handleLogout(authToken, result);
                return result;

            case "quit":
                result.quit = true;
                return result;

            default:
                System.out.println("Unknown command. Type 'help'.");
                return result;
        }
    }

    private void handleLogout(String authToken, PostloginResult out) {
        try {
            server.logout(authToken);
            System.out.println("Logged out.");
            out.loggedOut = true;
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void printHelp() {
        System.out.println("""
                help    - show commands
                logout  - log out
                quit    - exit program
                """);
    }
}
