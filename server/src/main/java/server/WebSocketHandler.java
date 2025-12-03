package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
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

            ws.onConnect(ctx -> System.out.println("WS connected: " + ctx.sessionId()));

            ws.onClose(manager::removeSession);
            ws.onError(manager::removeSession);

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
            case MAKE_MOVE -> handleMakeMove(ctx, gson.fromJson(rawJson, MakeMoveCommand.class));
            case LEAVE -> handleLeave(ctx, cmd);
            case RESIGN -> handleResign(ctx, cmd);
            case HIGHLIGHT -> handleHighlight(ctx, gson.fromJson(rawJson, HighlightCommand.class));
        }
    }

    private void handleConnect(WsContext ctx, UserGameCommand cmd) {
        try {
            AuthData auth = dao.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: bad auth");
                return;
            }

            int gameID = cmd.getGameID();
            GameData game = dao.getGame(gameID);
            if (game == null) {
                sendError(ctx, "Error: bad game id");
                return;
            }

            String username = auth.username();
            String role =
                    username.equals(game.whiteUsername()) ? "white" :
                            username.equals(game.blackUsername()) ? "black" : "observer";

            manager.addSession(ctx, username, gameID, role);

            manager.sendToSession(ctx.sessionId(), new LoadGameMessage(game.game()));

            manager.broadcastToGameExcept(ctx, gameID,
                    new NotificationMessage(username + " connected to game"));

        } catch (Exception e) {
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
                        game.gameName(), game.game(), game.gameOver(), null, null);
            } else if (username.equals(game.blackUsername())) {
                updated = new GameData(game.gameID(), game.whiteUsername(), null,
                        game.gameName(), game.game(), game.gameOver(), null, null);
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
                    game, gameData.gameOver(), null, null);

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
                        gameData.gameName(), game, true, null, null));
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
                    game.game(), true, null, null));

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
                    game.gameName(), game.game(), game.gameOver(),
                    pos, valid);

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
