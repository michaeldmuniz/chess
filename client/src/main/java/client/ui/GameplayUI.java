package client.ui;

import client.websocket.WebSocketClient;
import client.gameplay.GameplayState;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import chess.*;

import java.util.Scanner;

public class GameplayUI {

    private final WebSocketClient ws;
    private final GameplayState state;
    private final Scanner scanner;

    private final String authToken;
    private final int gameID;

    public GameplayUI(WebSocketClient ws,
                      GameplayState state,
                      Scanner scanner,
                      String authToken,
                      int gameID) {
        this.ws = ws;
        this.state = state;
        this.scanner = scanner;
        this.authToken = authToken;
        this.gameID = gameID;
    }

    public void run() {

        System.out.println("Entered gameplay mode. Type 'help' for commands.");
        BoardPrinter printer = new BoardPrinter();

        while (state.isRunning()) {

            if (state.shouldRedraw()) {
                ChessGame game = state.getGame();
                if (game != null) {
                    printer.drawBoard(
                            game,
                            state.isWhitePerspective(),
                            state.getHighlightOrigin(),
                            state.getHighlightMoves()
                    );
                }
                state.clearRedraw();
            }

            System.out.print("[GAME] >>> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "help"       -> printHelp();
                case "redraw"     -> state.markRedraw();

                case "leave"      -> {
                    ws.sendCommand(new UserGameCommand(
                            UserGameCommand.CommandType.LEAVE,
                            authToken,
                            gameID
                    ));
                    state.stop();
                }

                case "resign"     -> {
                    ws.sendCommand(new UserGameCommand(
                            UserGameCommand.CommandType.RESIGN,
                            authToken,
                            gameID
                    ));
                    state.stop();
                }

                case "move"       -> handleMove(parts);
                case "highlight"  -> handleHighlight(parts);

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
            ChessMove move = new ChessMove(from, to, null);

            ws.sendCommand(new MakeMoveCommand(authToken, gameID, move));

        } catch (Exception ex) {
            System.out.println("Invalid move. Example: move e2 e4");
        }
    }

    private void handleHighlight(String[] parts) {
        if (parts.length != 2) {
            System.out.println("Usage: highlight <square>");
            return;
        }

        try {
            ChessPosition pos = parsePos(parts[1]);
            ChessGame game = state.getGame();

            if (game == null) {
                System.out.println("Board not loaded yet.");
                return;
            }

            var moves = game.validMoves(pos);

            if (moves == null || moves.isEmpty()) {
                System.out.println("No legal moves from " + parts[1]);
                state.setHighlight(null, null);
            } else {
                state.setHighlight(pos, moves);
            }

            state.markRedraw();

        } catch (Exception ex) {
            System.out.println("Invalid square. Example: highlight b1");
        }
    }



    private void printHelp() {
        System.out.println("""
            Commands:
              help              - show this help text
              redraw            - redraw the board
              move <FROM> <TO>  - make a move (example: move e2 e4)
              highlight <FROM>  - highlight legal moves
              leave             - leave the game
              resign            - resign from the game
        """);
    }

    private ChessPosition parsePos(String txt) {
        txt = txt.toLowerCase();
        int col = txt.charAt(0) - 'a' + 1;
        int row = txt.charAt(1) - '0';
        return new ChessPosition(row, col);
    }
}
