package client.ui;

import client.ServerFacade;
import client.dto.RegisterRequest;
import client.dto.RegisterResponse;
import client.dto.LoginRequest;
import client.dto.LoginResponse;

import java.io.IOException;
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

    private void handleRegister(String[] parts) {
        if (parts.length != 4) {
            System.out.println("Usage: register <USERNAME> <PASSWORD> <EMAIL>");
            return;
        }

        String username = parts[1];
        String password = parts[2];
        String email = parts[3];

        try {
            RegisterRequest req = new RegisterRequest(username, password, email);
            RegisterResponse res = server.register(req);

            System.out.println("Registered successfully! AuthToken: " + res.authToken);

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }
    
    private void handleLogin(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Usage: login <USERNAME> <PASSWORD>");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        try {
            LoginRequest req = new LoginRequest(username, password);
            LoginResponse res = server.login(req);

            // Still not switching to Postlogin UI
            System.out.println("Logged in successfully! AuthToken: " + res.authToken());

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
