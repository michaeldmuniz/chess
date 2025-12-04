package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import model.AuthData;
import model.GameData;
import websocket.commands.HighlightCommand;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

public class WebSocketHandler {

    private final WebSocketConnectionManager manager = new WebSocketConnectionManager();
    private final DataAccess dao;
    private final Gson gson = new Gson();

    public WebSocketHandler(DataAccess dao) {
        this.dao = dao;
    }

    public void configure(Javalin app) {
        System.out.println("[SERVER] Configuring WebSocket endpoint at /ws");
        try {
            app.ws("/ws", ws -> {
                ws.onConnect(ctx -> {
                    try {
                        // Enable automatic pings to prevent idle timeout (as per documentation)
                        ctx.enableAutomaticPings();
                        
                        System.out.println("========================================");
                        System.out.println("[SERVER-DEBUG] WebSocket connection established!");
                        System.out.println("[SERVER-DEBUG] Session ID: " + ctx.sessionId());
                        System.out.println("[SERVER-DEBUG] Session isOpen: " + ctx.session.isOpen());
                        System.out.println("[SERVER-DEBUG] Remote address: " + ctx.session.getRemoteAddress());
                        System.out.println("[SERVER-DEBUG] Protocol version: " + ctx.session.getProtocolVersion());
                        System.out.println("========================================");
                    } catch (Exception e) {
                        System.out.println("[SERVER-DEBUG] Exception in onConnect: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

                ws.onClose(ctx -> {
                    System.out.println("[SERVER-DEBUG] onClose() called for session: " + ctx.sessionId());
                    System.out.println("[SERVER-DEBUG] Close status: " + ctx.status());
                    System.out.println("[SERVER-DEBUG] Close reason: " + ctx.reason());
                    manager.removeSession(ctx);
                });
                ws.onError(ctx -> {
                    System.out.println("[SERVER-DEBUG] onError() called for session: " + ctx.sessionId());
                    System.out.println("[SERVER-DEBUG] Error: " + ctx.error());
                    if (ctx.error() != null) {
                        ctx.error().printStackTrace();
                    }
                    manager.removeSession(ctx);
                });

                ws.onMessage(ctx -> {
                    try {
                        System.out.println("[SERVER-DEBUG] Message received from session: " + ctx.sessionId());
                        String rawJson = ctx.message();
                        System.out.println("[SERVER-DEBUG] Raw JSON: " + rawJson);
                        System.out.println("[SERVER-DEBUG] JSON length: " + (rawJson != null ? rawJson.length() : 0));

                        UserGameCommand cmd = gson.fromJson(rawJson, UserGameCommand.class);
                        System.out.println("[SERVER-DEBUG] Parsed command: " + (cmd != null ? cmd.getCommandType() : "null"));

                        if (cmd == null || cmd.getCommandType() == null) {
                            System.out.println("[SERVER-DEBUG] Invalid command, sending error");
                            sendError(ctx, "Error: invalid command");
                            return;
                        }

                        System.out.println("[SERVER-DEBUG] Routing command: " + cmd.getCommandType());
                        routeCommand(ctx, cmd, rawJson);
                    } catch (Exception e) {
                        System.out.println("[SERVER-DEBUG] Exception in onMessage: " + e.getMessage());
                        e.printStackTrace();
                        try {
                            sendError(ctx, "Error: " + e.getMessage());
                        } catch (Exception ex) {
                            System.out.println("[SERVER-DEBUG] Failed to send error: " + ex.getMessage());
                        }
                    }
                });
            });
        } catch (Exception e) {
            System.out.println("[SERVER] Exception configuring WebSocket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void routeCommand(WsContext ctx, UserGameCommand cmd, String rawJson) {
        switch (cmd.getCommandType()) {
            case CONNECT -> handleConnect(ctx, cmd);
            case MAKE_MOVE -> handleMakeMove(ctx, gson.fromJson(rawJson, MakeMoveCommand.class));
            case LEAVE -> handleLeave(ctx, cmd);
            case RESIGN -> handleResign(ctx, cmd);
            case HIGHLIGHT -> handleHighlight(ctx, gson.fromJson(rawJson, HighlightCommand.class));
        }
    }

    private void handleConnect(WsContext ctx, UserGameCommand cmd) {
        System.out.println("[SERVER-DEBUG] handleConnect() called");
        System.out.println("[SERVER-DEBUG] Session ID: " + ctx.sessionId());
        System.out.println("[SERVER-DEBUG] Game ID: " + cmd.getGameID());
        try {
            AuthData auth = dao.getAuth(cmd.getAuthToken());
            if (auth == null) {
                System.out.println("[SERVER-DEBUG] Auth failed");
                sendError(ctx, "Error: bad auth");
                return;
            }
            System.out.println("[SERVER-DEBUG] Auth successful, username: " + auth.username());

            int gameID = cmd.getGameID();
            GameData game = dao.getGame(gameID);
            if (game == null) {
                System.out.println("[SERVER-DEBUG] Game not found: " + gameID);
                sendError(ctx, "Error: bad game id");
                return;
            }
            System.out.println("[SERVER-DEBUG] Game found: " + game.gameName());

            String username = auth.username();
            String role =
                    username.equals(game.whiteUsername()) ? "white" :
                            username.equals(game.blackUsername()) ? "black" : "observer";
            System.out.println("[SERVER-DEBUG] User role: " + role);

            manager.addSession(ctx, username, gameID, role);
            System.out.println("[SERVER-DEBUG] Session added to manager");

            System.out.println("[SERVER-DEBUG] Sending LoadGameMessage...");
            manager.sendToSession(ctx.sessionId(), new LoadGameMessage(game.game()));
            System.out.println("[SERVER-DEBUG] LoadGameMessage sent");

            System.out.println("[SERVER-DEBUG] Broadcasting notification...");
            manager.broadcastToGameExcept(ctx, gameID,
                    new NotificationMessage(username + " connected to game"));
            System.out.println("[SERVER-DEBUG] Notification broadcasted");

        } catch (Exception e) {
            System.out.println("[SERVER-DEBUG] Exception in handleConnect: " + e.getMessage());
            e.printStackTrace();
            sendError(ctx, "Error: " + e.getMessage());
        }
    }


    private void handleLeave(WsContext ctx, UserGameCommand cmd) {
        try {
            AuthData auth = dao.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: unauthorized");
                return;
            }

            int gameID = cmd.getGameID();
            GameData game = dao.getGame(gameID);
            if (game == null) {
                sendError(ctx, "Error: bad game id");
                return;
            }

            String username = auth.username();
            GameData updated = game;

            if (username.equals(game.whiteUsername())) {
                updated = new GameData(game.gameID(), null, game.blackUsername(),
                        game.gameName(), game.game(), game.gameOver());
            } else if (username.equals(game.blackUsername())) {
                updated = new GameData(game.gameID(), game.whiteUsername(), null,
                        game.gameName(), game.game(), game.gameOver());
            }

            if (updated != game) dao.updateGame(updated);

            manager.removeSession(ctx);

            // Others get NOTIFICATION
            manager.broadcastToGame(gameID,
                    new NotificationMessage(username + " left the game"));

        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }


    private void handleMakeMove(WsContext ctx, MakeMoveCommand cmd) {
        try {
            AuthData auth = dao.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: bad auth");
                return;
            }

            int gameID = cmd.getGameID();
            GameData gameData = dao.getGame(gameID);
            if (gameData == null) {
                sendError(ctx, "Error: bad game id");
                return;
            }

            if (gameData.gameOver()) {
                sendError(ctx, "Error: game over");
                return;
            }

            String username = auth.username();
            ChessGame game = gameData.game();

            ChessGame.TeamColor color =
                    username.equals(gameData.whiteUsername()) ? ChessGame.TeamColor.WHITE :
                            username.equals(gameData.blackUsername()) ? ChessGame.TeamColor.BLACK : null;

            if (color == null) {
                sendError(ctx, "Error: observers cannot move");
                return;
            }

            if (game.getTeamTurn() != color) {
                sendError(ctx, "Error: not your turn");
                return;
            }

            var move = cmd.getMove();
            var piece = game.getBoard().getPiece(move.getStartPosition());

            if (piece == null || piece.getTeamColor() != color) {
                sendError(ctx, "Error: cannot move opponent piece");
                return;
            }

            var legal = game.validMoves(move.getStartPosition());
            if (legal == null || !legal.contains(move)) {
                sendError(ctx, "Error: invalid move");
                return;
            }

            game.makeMove(move);

            GameData updated = new GameData(gameID,
                    gameData.whiteUsername(), gameData.blackUsername(), gameData.gameName(),
                    game, gameData.gameOver());

            dao.updateGame(updated);


            manager.sendToSession(ctx.sessionId(), new LoadGameMessage(game));

            manager.broadcastToGameExcept(ctx, gameID, new LoadGameMessage(game));
            manager.broadcastToGameExcept(ctx, gameID,
                    new NotificationMessage(username + " made a move"));

            if (game.isInCheckmate(ChessGame.TeamColor.WHITE)
                    || game.isInCheckmate(ChessGame.TeamColor.BLACK)) {

                manager.broadcastToGame(gameID, new NotificationMessage("Checkmate!"));

                dao.updateGame(new GameData(gameID,
                        gameData.whiteUsername(), gameData.blackUsername(),
                        gameData.gameName(), game, true));
            }

        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }


    private void handleResign(WsContext ctx, UserGameCommand cmd) {
        try {
            AuthData auth = dao.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: unauthorized");
                return;
            }

            int gameID = cmd.getGameID();
            GameData game = dao.getGame(gameID);
            if (game == null) {
                sendError(ctx, "Error: bad game id");
                return;
            }

            if (game.gameOver()) {
                sendError(ctx, "Error: game already over");
                return;
            }

            String username = auth.username();

            boolean isPlayer =
                    username.equals(game.whiteUsername())
                            || username.equals(game.blackUsername());

            if (!isPlayer) {
                sendError(ctx, "Error: observers cannot resign");
                return;
            }

            dao.updateGame(new GameData(gameID,
                    game.whiteUsername(), game.blackUsername(), game.gameName(),
                    game.game(), true));

            NotificationMessage msg = new NotificationMessage(username + " resigned");
            manager.sendToSession(ctx.sessionId(), msg);

            manager.broadcastToGameExcept(ctx, gameID, msg);

        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }


    private void handleHighlight(WsContext ctx, HighlightCommand cmd) {
        try {
            AuthData auth = dao.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: unauthorized");
                return;
            }

            int gameID = cmd.getGameID();
            GameData game = dao.getGame(gameID);
            if (game == null) {
                sendError(ctx, "Error: bad game id");
                return;
            }

            var pos = cmd.getPosition();
            var valid = game.game().validMoves(pos);
            if (valid == null) valid = java.util.Set.of();

            GameData updated = new GameData(gameID,
                    game.whiteUsername(), game.blackUsername(),
                    game.gameName(), game.game(), game.gameOver());

            dao.updateGame(updated);

            manager.sendToSession(ctx.sessionId(),
                    new LoadGameMessage(game.game()));

        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }
    private void sendError(WsContext ctx, String msg) {
        if (!msg.startsWith("Error")) msg = "Error: " + msg;
        ctx.send(gson.toJson(new ErrorMessage(msg)));
    }
}
