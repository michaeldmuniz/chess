package client.ui;


import client.ServerFacade;
import client.dto.CreateGameRequest;
import client.dto.JoinGameRequest;
import client.dto.ListGamesRequest;
import client.dto.ListGamesResponse;
import model.GameData;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class PostloginUI {

    private final ServerFacade server;
    private final Scanner scanner;
    private List<GameData> lastListedGames = List.of();

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

            case "play":
                handlePlay(parts, authToken, result);
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
            this.lastListedGames = games;

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

    private void handlePlay(String[] parts, String authToken, PostloginResult out) {
        // Must have: play <GAME_NUMBER> <WHITE|BLACK>
        if (parts.length < 3) {
            System.out.println("Usage: play <GAME_NUMBER> <WHITE|BLACK>");
            return;
        }

        // Parse GAME_NUMBER
        int index;
        try {
            index = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            System.out.println("Game number must be an integer.");
            return;
        }

        // Validate that listGames was run at least once
        if (lastListedGames == null || lastListedGames.isEmpty()) {
            System.out.println("No games loaded. Run 'list' first.");
            return;
        }

        // Validate range
        if (index < 1 || index > lastListedGames.size()) {
            System.out.println("Invalid game number.");
            return;
        }

        // Now retrieve the GameData
        var game = lastListedGames.get(index - 1);

        // Validate the color
        String colorInput = parts[2].toUpperCase();
        if (!colorInput.equals("WHITE") && !colorInput.equals("BLACK")) {
            System.out.println("Color must be WHITE or BLACK.");
            return;
        }

        // For now just confirm parsed data
        System.out.println("Parsed play request:");
        System.out.println("  Game #" + index + " -> ID " + game.gameID());
        System.out.println("  Color: " + colorInput);

        // Build request to join the game
        var joinReq = new JoinGameRequest(colorInput, game.gameID());

        try {
            server.joinGame(joinReq, authToken);
            System.out.println("Successfully joined game " + game.gameID() + " as " + colorInput + ".");

            // Draw initial board (white perspective only for now)
            var printer = new BoardPrinter();
            printer.drawBoard(game.game());

        } catch (IOException ex) {
            System.out.println("Join game failed: " + ex.getMessage());
        }
    }

    private void printHelp() {
        System.out.println("""
        help      - show commands
        list      - list all games
        create    - create a new game
        play      - join a game as white or black
        logout    - log out
        quit      - exit program
        """);

    }

    public List<GameData> getLastListedGames() {
        return lastListedGames;
    }

}
