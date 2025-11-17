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
    private final String currentUsername;   // ★ Added
    private List<GameData> lastListedGames = List.of();

    public PostloginUI(ServerFacade server, Scanner scanner, String currentUsername) {
        this.server = server;
        this.scanner = scanner;
        this.currentUsername = currentUsername;   // ★ Store logged-in username
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

            case "observe":
                handleObserve(parts, authToken);
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
            System.out.println("Successfully created game: " + name);
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }

    }

    private void handlePlay(String[] parts, String authToken, PostloginResult out) {
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

        // Retrieve GameData
        var game = lastListedGames.get(index - 1);

        // Validate color
        String colorInput = parts[2].toUpperCase();
        if (!colorInput.equals("WHITE") && !colorInput.equals("BLACK")) {
            System.out.println("Color must be WHITE or BLACK.");
            return;
        }


        if (colorInput.equals("WHITE")
                && game.whiteUsername() != null
                && game.whiteUsername().equals(currentUsername)) {

            System.out.println("Re-entering game as WHITE...");
            boolean whitePerspective = true;

            var printer = new BoardPrinter();
            printer.drawBoard(game.game(), whitePerspective);

            out.enteredGame = true;
            out.joinedGame = game;
            out.joinColor = colorInput;
            return;
        }

        if (colorInput.equals("BLACK")
                && game.blackUsername() != null
                && game.blackUsername().equals(currentUsername)) {

            System.out.println("Re-entering game as BLACK...");
            boolean whitePerspective = false;

            var printer = new BoardPrinter();
            printer.drawBoard(game.game(), whitePerspective);

            out.enteredGame = true;
            out.joinedGame = game;
            out.joinColor = colorInput;
            return;
        }


        System.out.println("Parsed play request:");
        System.out.println("  Game #" + index + " -> ID " + game.gameID());
        System.out.println("  Color: " + colorInput);

        var joinReq = new JoinGameRequest(colorInput, game.gameID());

        try {
            server.joinGame(joinReq, authToken);

            System.out.println("Successfully joined game " + game.gameID() + " as " + colorInput + ".");
            boolean whitePerspective = colorInput.equals("WHITE");

            var printer = new BoardPrinter();
            printer.drawBoard(game.game(), whitePerspective);

            out.enteredGame = true;
            out.joinedGame = game;
            out.joinColor = colorInput;

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
        observe   - observe a game
        logout    - log out
        quit      - exit program
        """);
    }

    public List<GameData> getLastListedGames() {
        return lastListedGames;
    }

    private void handleObserve(String[] parts, String authToken) {

        // Usage: observe <GAME_NUMBER>
        if (parts.length < 2) {
            System.out.println("Usage: observe <GAME_NUMBER>");
            return;
        }

        // Parse game number
        int index;
        try {
            index = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            System.out.println("Game number must be an integer.");
            return;
        }

        // Must have run list first
        if (lastListedGames == null || lastListedGames.isEmpty()) {
            System.out.println("No games loaded. Run 'list' first.");
            return;
        }

        if (index < 1 || index > lastListedGames.size()) {
            System.out.println("Invalid game number.");
            return;
        }

        // Retrieve game
        var game = lastListedGames.get(index - 1);

        System.out.println("Observing game \"" + game.gameName() + "\"");

        // Draw board *from white perspective* (Phase 5 rules)
        var printer = new BoardPrinter();
        printer.drawBoard(game.game(), true);
    }


}
