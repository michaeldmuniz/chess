package client.ui;

import client.ServerFacade;
import client.dto.CreateGameRequest;
import client.dto.ListGamesRequest;
import client.dto.ListGamesResponse;
import model.GameData;

import java.io.IOException;
import java.util.List;
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

            case "list":
                handleListGames(authToken);
                return result;

            case "create":
                handleCreate(authToken);
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

    private void handleListGames(String authToken) {
        try {
            ListGamesRequest req = new ListGamesRequest(authToken);
            ListGamesResponse res = server.listGames(req);

            List<GameData> games = res.games();

            if (games == null || games.isEmpty()) {
                System.out.println("No games found.");
                return;
            }

            int num = 1;
            for (GameData g : games) {

                String white = (g.whiteUsername() == null)
                        ? "-" : g.whiteUsername();

                String black = (g.blackUsername() == null)
                        ? "-" : g.blackUsername();

                System.out.println(num + ". \"" + g.gameName() +
                        "\"   white=" + white +
                        "   black=" + black);

                num++;
            }

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void handleCreate(String authToken) {
        System.out.print("Enter game name: ");
        String name = scanner.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Game name cannot be empty.");
            return;
        }

        try {
            var req = new CreateGameRequest(name, authToken);
            var resp = server.createGame(req);
            System.out.println("Created game with ID: " + resp.gameID());
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }


    private void printHelp() {
        System.out.println("""
        help      - show commands
        list      - list all games
        create    - create a new game
        logout    - log out
        quit      - exit program
        """);

    }
}
