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

    public PreloginResult runOnce() {
        PreloginResult result = new PreloginResult();

        System.out.print("[LOGGED_OUT] >>> ");
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

            case "quit":
                result.quit = true;
                return result;

            case "register":
                handleRegister(parts, result);
                return result;

            case "login":
                handleLogin(parts, result);
                return result;

            default:
                System.out.println("Unknown command. Type 'help'.");
                return result;
        }
    }

    private void handleRegister(String[] parts, PreloginResult out) {
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

            System.out.println("Registered successfully! Type help to continue");
            out.loggedIn = true;
            out.authToken = res.authToken;
            out.username = res.username;

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void handleLogin(String[] parts, PreloginResult out) {
        if (parts.length != 3) {
            System.out.println("Usage: login <USERNAME> <PASSWORD>");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        try {
            LoginRequest req = new LoginRequest(username, password);
            LoginResponse res = server.login(req);

            System.out.println("Logged in successfully! Type help to continue");
            out.loggedIn = true;
            out.authToken = res.authToken();
            out.username = res.username();

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
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
}
