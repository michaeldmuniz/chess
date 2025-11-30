package client.ui;

import client.websocket.WebSocketClient;
import client.gameplay.GameplayState;
import chess.*;
import java.util.Scanner;

public class GameplayUI {

    private final WebSocketClient ws;
    private final GameplayState state;
    private final Scanner scanner;

    public GameplayUI(WebSocketClient ws, GameplayState state, Scanner scanner) {
        this.ws = ws;
        this.state = state;
        this.scanner = scanner;
    }

    public void run() {

        System.out.println("Entered gameplay mode. Type 'help' for commands.");

        BoardPrinter printer = new BoardPrinter();

        while (state.isRunning()) {

            ChessGame game = state.getGame();
            if (game != null) {
                printer.drawBoard(game, state.isWhitePerspective());
            }

            System.out.print("[GAME] >>> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "help" -> printHelp();
                case "redraw" -> { /* loop will auto redraw next iteration */ }

                case "leave" -> {
                    ws.sendCommand(GameplayCommands.leave());
                    state.stop();
                }

                case "resign" -> {
                    ws.sendCommand(GameplayCommands.resign());
                    state.stop();
                }

                case "move" -> handleMove(parts);
                case "highlight" -> handleHighlight(parts);

                default -> System.out.println("Unknown command. Type 'help'");
            }
        }

        System.out.println("Exiting game...");
    }

    private void handleMove(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Usage: move <from> <to>");
            return;
        }

        try {
            ChessPosition from = parsePos(parts[1]);
            ChessPosition to = parsePos(parts[2]);

            ws.sendCommand(GameplayCommands.move(from, to));

        } catch (Exception ex) {
            System.out.println("Invalid coordinates. Example: move e2 e4");
        }
    }

    private void handleHighlight(String[] parts) {
        if (parts.length != 2) {
            System.out.println("Usage: highlight <square>");
            return;
        }

        try {
            ChessPosition pos = parsePos(parts[1]);
            ws.sendCommand(GameplayCommands.highlight(pos));

        } catch (Exception ex) {
            System.out.println("Invalid square. Example: highlight b1");
        }
    }

    private ChessPosition parsePos(String txt) {
        txt = txt.toLowerCase();
        int col = txt.charAt(0) - 'a' + 1;
        int row = txt.charAt(1) - '0';
        return new ChessPosition(row, col);
    }

    private void printHelp() {
        System.out.println("""
            Commands:
              help
              redraw
              move <FROM> <TO>
              highlight <FROM>
              leave
              resign
        """);
    }
}