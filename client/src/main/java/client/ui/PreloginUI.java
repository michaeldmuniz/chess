package client.ui;

import client.ServerFacade;

import java.util.Scanner;

public class PreloginUI {

    private final ServerFacade server;
    private final Scanner scanner;

    public PreloginUI(ServerFacade server, Scanner scanner) {
        this.server = server;
        this.scanner = scanner;
    }

    public boolean runOnce() {
        System.out.print("[LOGGED_OUT] >>> ");
        String line = scanner.nextLine().trim();

        if (line.isEmpty()) {
            return false;
        }

        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "help":
                printHelp();
                return false;

            case "quit":
                return true;

            case "register":
                handleRegister(parts);
                return false;

            case "login":
                handleLogin(parts);
                return false;

            default:
                System.out.println("Unknown command. Type 'help'.");
                return false;
        }
    }

    private void printHelp() {
        System.out.println("""
                register <USERNAME> <PASSWORD> <EMAIL> - to create an account
                login <USERNAME> <PASSWORD>            - to play chess
                quit                                   - exit the program
                help                                   - show possible commands
                """);
    }

    /**
     * Placeholder
     * No ServerFacade call yet.
     */
    private void handleRegister(String[] parts) {
        if (parts.length != 4) {
            System.out.println("Usage: register <USERNAME> <PASSWORD> <EMAIL>");
            return;
        }

        // Temporary placeholder
        String user = parts[1];
        String pass = parts[2];
        String email = parts[3];

        System.out.println("(pretend register) user=" + user + " email=" + email);
    }

    /**
     * Placeholder
     * No ServerFacade call yet.
     */
    private void handleLogin(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Usage: login <USERNAME> <PASSWORD>");
            return;
        }

        // Temporary placeholder (will integrate ServerFacade)
        String user = parts[1];
        System.out.println("(pretend login) user=" + user);
    }
}
