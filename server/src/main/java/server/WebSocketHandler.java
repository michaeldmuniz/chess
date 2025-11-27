package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import model.AuthData;
import model.GameData;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

public class WebSocketHandler {

    private final WebSocketConnectionManager manager = new WebSocketConnectionManager();
    private final DataAccess dao;
    private final Gson gson = new Gson();

    public WebSocketHandler(DataAccess dao) {
        this.dao = dao;
    }

    public void configure(Javalin app) {
        app.ws("/ws", ws -> {

            ws.onConnect(ctx -> {
                System.out.println("WS connected: " + ctx.sessionId());
            });

            ws.onClose(ctx -> {
                manager.removeSession(ctx);
            });

            ws.onError(ctx -> {
                manager.removeSession(ctx);
            });

            ws.onMessage(ctx -> {
                String rawJson = ctx.message();

                UserGameCommand cmd = gson.fromJson(rawJson, UserGameCommand.class);
                if (cmd == null || cmd.getCommandType() == null) {
                    sendError(ctx, "Error: invalid command");
                    return;
                }

                routeCommand(ctx, cmd, rawJson);
            });
        });
    }

    private void routeCommand(WsContext ctx, UserGameCommand cmd, String rawJson) {
        switch (cmd.getCommandType()) {
            case CONNECT -> handleConnect(ctx, cmd);

            case MAKE_MOVE -> {
                MakeMoveCommand moveCmd = gson.fromJson(rawJson, MakeMoveCommand.class);
                handleMakeMove(ctx, moveCmd);
            }

            case LEAVE -> handleLeave(ctx, cmd);

            case RESIGN -> handleResign(ctx, cmd);
        }
    }

    private void handleConnect(WsContext ctx, UserGameCommand cmd) {
        try {
            // Validate auth token
            var auth = dao.getAuth(cmd.getAuthToken());
            if (auth == null) {
                manager.sendToSession(ctx.sessionId(),
                        new ErrorMessage("Error: bad auth"));
                return;
            }

            String username = auth.username();
            int gameID = cmd.getGameID();

            // Validate game exists
            var gameData = dao.getGame(gameID);
            if (gameData == null) {
                manager.sendToSession(ctx.sessionId(),
                        new ErrorMessage("Error: bad game ID"));
                return;
            }

            // Determine user's role in this game
            String role;
            if (username.equals(gameData.whiteUsername())) {
                role = "white";
            } else if (username.equals(gameData.blackUsername())) {
                role = "black";
            } else {
                role = "observer";
            }

            // Register WebSocket session
            manager.addSession(ctx, username, gameID, role);

            // Send LOAD_GAME back to this user
            LoadGameMessage load = new LoadGameMessage(gameData.game());
            manager.sendToSession(ctx.sessionId(), load);

            // Notify other players
            NotificationMessage note =
                    new NotificationMessage(username + " connected to game");
            manager.broadcastToGameExcept(ctx, gameID, note);

        } catch (Exception e) {
            manager.sendToSession(ctx.sessionId(),
                    new ErrorMessage("Error: " + e.getMessage()));
        }
    }


    private void handleLeave(WsContext ctx, UserGameCommand cmd) {
        try {
            // Validate auth
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

            // If this user is a player, remove them from the game in the DB
            GameData updatedGame = game;

            if (username.equals(game.whiteUsername())) {
                updatedGame = new GameData(
                        game.gameID(),
                        null,                         // white leaves
                        game.blackUsername(),
                        game.gameName(),
                        game.game()
                );
            } else if (username.equals(game.blackUsername())) {
                updatedGame = new GameData(
                        game.gameID(),
                        game.whiteUsername(),
                        null,                         // black leaves
                        game.gameName(),
                        game.game()
                );
            }

            // Only call updateGame if we actually changed something
            if (updatedGame != game) {
                dao.updateGame(updatedGame);
            }

            // Remove this websocket session from our in-memory maps
            manager.removeSession(ctx);

            // Notify remaining clients in this game
            ServerMessage note = new NotificationMessage(username + " left the game");
            manager.broadcastToGame(gameID, note);

            // Spec/tests say: leaving user does NOT get a notification is done

        } catch (DataAccessException e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }


    private void handleMakeMove(WsContext ctx, MakeMoveCommand cmd) {
        sendError(ctx, "Error: MAKE_MOVE not implemented yet");
    }

    private void handleResign(WsContext ctx, UserGameCommand cmd) {
        sendError(ctx, "Error: RESIGN not implemented yet");
    }


    private void sendError(WsContext ctx, String message) {
        // Ensure the word "Error" appears
        if (!message.toLowerCase().contains("error")) {
            message = "Error: " + message;
        }
        ErrorMessage err = new ErrorMessage(message);
        ctx.send(gson.toJson(err));
    }
}
