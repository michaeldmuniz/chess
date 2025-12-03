package client.ui;

import client.ServerFacade;
import client.dto.CreateGameRequest;
import client.dto.JoinGameRequest;
import client.dto.ListGamesRequest;
import client.dto.ListGamesResponse;
import model.GameData;

import client.websocket.WebSocketClient;
import client.gameplay.GameplayState;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class PostloginUI {

    private final ServerFacade server;
    private final Scanner scanner;
    private final String currentUsername;

    private List<GameData> lastListedGames = List.of();

    public PostloginUI(ServerFacade server, Scanner scanner, String currentUsername) {
        this.server = server;
        this.scanner = scanner;
        this.currentUsername = currentUsername;
    }

    public PostloginResult runOnce(String authToken) {
        PostloginResult result = new PostloginResult();

        System.out.print("[LOGGED_IN] >>> ");
        String line = scanner.nextLine().trim();

        if (line.isEmpty()) return result;

        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "help" -> printHelp();
            case "logout" -> handleLogout(authToken, result);
            case "list" -> handleListGames(authToken);
            case "create" -> handleCreate(authToken);
            case "play" -> handlePlay(parts, authToken);
            case "observe" -> handleObserve(parts, authToken);
            case "quit" -> result.quit = true;
            default -> System.out.println("Unknown command. Type 'help'.");
        }

        return result;
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

            lastListedGames = res.games();

            if (lastListedGames == null || lastListedGames.isEmpty()) {
                System.out.println("No games found.");
                return;
            }

            int i = 1;
            for (GameData g : lastListedGames) {
                String white = g.whiteUsername() == null ? "-" : g.whiteUsername();
                String black = g.blackUsername() == null ? "-" : g.blackUsername();

                System.out.println(i + ". \"" + g.gameName() + "\" white=" + white + " black=" + black);
                i++;
            }

        } catch (Exception e) {
            System.out.println("Error listing games: " + e.getMessage());
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
            CreateGameRequest req = new CreateGameRequest(name, authToken);
            server.createGame(req);
            System.out.println("Created game \"" + name + "\"");
        } catch (Exception e) {
            System.out.println("Error creating game: " + e.getMessage());
        }
    }

    private void handlePlay(String[] parts, String authToken) {
        if (parts.length < 3) {
            System.out.println("Usage: play <GAME_NUMBER> <WHITE|BLACK>");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            System.out.println("Invalid game number.");
            return;
        }

        if (index < 1 || index > lastListedGames.size()) {
            System.out.println("Invalid game number.");
            return;
        }

        GameData game = lastListedGames.get(index - 1);

        String color = parts[2].toUpperCase();
        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            System.out.println("Color must be WHITE or BLACK.");
            return;
        }

        boolean whitePerspective;

        // Re-entering game
        if (color.equals("WHITE") && currentUsername.equals(game.whiteUsername())) {
            System.out.println("Re-entering as WHITE...");
            whitePerspective = true;

        } else if (color.equals("BLACK") && currentUsername.equals(game.blackUsername())) {
            System.out.println("Re-entering as BLACK...");
            whitePerspective = false;

        } else {
            // Must join the game via HTTP
            try {
                server.joinGame(new JoinGameRequest(color, game.gameID()), authToken);
                System.out.println("Joined game as " + color);
            } catch (Exception e) {
                System.out.println("Join failed: " + e.getMessage());
                return;
            }
            whitePerspective = color.equals("WHITE");
        }

        startGameplay(game, whitePerspective, authToken);
    }

    private void startGameplay(GameData game, boolean whitePerspective, String authToken) {
        try {
            String wsUrl = "ws://localhost:8080/ws";

            GameplayState state = new GameplayState(whitePerspective);

            WebSocketClient ws = new WebSocketClient(wsUrl, new WebSocketClient.MessageHandler() {
                @Override
                public void onLoadGame(websocket.messages.LoadGameMessage msg) {
                    state.setGame(msg.getGame());
                }

                @Override
                public void onNotification(websocket.messages.NotificationMessage msg) {
                    System.out.println("[NOTIFY] " + msg.getMessage());
                }

                @Override
                public void onError(websocket.messages.ErrorMessage msg) {
                    System.out.println("[ERROR] " + msg.getErrorMessage());
                }
            });

            ws.attachState(state);
            ws.connect();

            ws.sendConnect(authToken, game.gameID());

            GameplayUI ui = new GameplayUI(ws, state, scanner, authToken, game.gameID());
            ui.run();

            ws.close();

        } catch (Exception e) {
            System.out.println("Failed to start gameplay: " + e.getMessage());
        }
    }

    private void handleObserve(String[] parts, String authToken) {
        if (parts.length < 2) {
            System.out.println("Usage: observe <GAME_NUMBER>");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            System.out.println("Invalid game number.");
            return;
        }

        if (index < 1 || index > lastListedGames.size()) {
            System.out.println("Invalid game number.");
            return;
        }

        GameData game = lastListedGames.get(index - 1);

        System.out.println("Observing \"" + game.gameName() + "\"");

        startGameplay(game, true, authToken);
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

}
