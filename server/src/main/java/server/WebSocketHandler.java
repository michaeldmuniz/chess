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
        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                // Enable automatic pings to prevent idle timeout (as per documentation)
                ctx.enableAutomaticPings();
            });

            ws.onClose(ctx -> {
                manager.removeSession(ctx);
            });

            ws.onError(ctx -> {
                manager.removeSession(ctx);
            });

            ws.onMessage(ctx -> {
                try {
                    String rawJson = ctx.message();
                    UserGameCommand cmd = gson.fromJson(rawJson, UserGameCommand.class);

                    if (cmd == null || cmd.getCommandType() == null) {
                        sendError(ctx, "Error: invalid command");
                        return;
                    }

                    routeCommand(ctx, cmd, rawJson);
                } catch (Exception e) {
                    try {
                        sendError(ctx, "Error: " + e.getMessage());
                    } catch (Exception ex) {
                        // Silently handle error sending errors
                    }
                }
            });
        });
    }

    private void routeCommand(WsContext ctx, UserGameCommand cmd, String rawJson) {
        switch (cmd.getCommandType()) {
            case CONNECT -> handleConnect(ctx, cmd);
            case MAKE_MOVE -> {
                // Check if user is connected and not an observer
                String role = manager.getRole(ctx);
                if (role == null) {
                    sendError(ctx, "Error: not connected");
                    return;
                }
                if ("observer".equals(role)) {
                    sendError(ctx, "Error: observers cannot move");
                    return;
                }
                handleMakeMove(ctx, gson.fromJson(rawJson, MakeMoveCommand.class));
            }
            case LEAVE -> handleLeave(ctx, cmd);
            case RESIGN -> {
                // Check if user is connected and not an observer
                String role = manager.getRole(ctx);
                if (role == null) {
                    sendError(ctx, "Error: not connected");
                    return;
                }
                if ("observer".equals(role)) {
                    sendError(ctx, "Error: observers cannot resign");
                    return;
                }
                handleResign(ctx, cmd);
            }
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

            // Send appropriate notification based on role
            String notification;
            if ("observer".equals(role)) {
                notification = username + " connected to the game as an observer";
            } else {
                notification = username + " connected to the game as " + role;
            }
            manager.broadcastToGameExcept(ctx, gameID, new NotificationMessage(notification));

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
                        game.gameName(), game.game(), game.gameOver());
            } else if (username.equals(game.blackUsername())) {
                updated = new GameData(game.gameID(), game.whiteUsername(), null,
                        game.gameName(), game.game(), game.gameOver());
            }

            if (updated != game) {
                dao.updateGame(updated);
            }

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

            // Format move description
            String moveDescription = formatMove(move, piece);
            manager.broadcastToGameExcept(ctx, gameID,
                    new NotificationMessage(username + " made a move: " + moveDescription));

            // Check for game end conditions and send notifications
            checkGameEndConditions(game, gameData, gameID);

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
            if (valid == null) {
                valid = java.util.Set.of();
            }

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
    private void checkGameEndConditions(ChessGame game, GameData gameData, int gameID) {
        try {
            // Check for checkmate first (checkmate implies check, so only send checkmate notification)
            if (game.isInCheckmate(ChessGame.TeamColor.WHITE)) {
                String playerName = gameData.whiteUsername();
                manager.broadcastToGame(gameID,
                        new NotificationMessage(playerName + " is in checkmate"));
                dao.updateGame(new GameData(gameID,
                        gameData.whiteUsername(), gameData.blackUsername(),
                        gameData.gameName(), game, true));
            } else if (game.isInCheckmate(ChessGame.TeamColor.BLACK)) {
                String playerName = gameData.blackUsername();
                manager.broadcastToGame(gameID,
                        new NotificationMessage(playerName + " is in checkmate"));
                dao.updateGame(new GameData(gameID,
                        gameData.whiteUsername(), gameData.blackUsername(),
                        gameData.gameName(), game, true));
            } else if (game.isInStalemate(ChessGame.TeamColor.WHITE)) {
                String playerName = gameData.whiteUsername();
                manager.broadcastToGame(gameID,
                        new NotificationMessage(playerName + " is in stalemate"));
                dao.updateGame(new GameData(gameID,
                        gameData.whiteUsername(), gameData.blackUsername(),
                        gameData.gameName(), game, true));
            } else if (game.isInStalemate(ChessGame.TeamColor.BLACK)) {
                String playerName = gameData.blackUsername();
                manager.broadcastToGame(gameID,
                        new NotificationMessage(playerName + " is in stalemate"));
                dao.updateGame(new GameData(gameID,
                        gameData.whiteUsername(), gameData.blackUsername(),
                        gameData.gameName(), game, true));
            } else {
                // Only send check notification if not in checkmate or stalemate
                ChessGame.TeamColor currentTurn = game.getTeamTurn();
                if (game.isInCheck(currentTurn)) {
                    String playerName = (currentTurn == ChessGame.TeamColor.WHITE)
                            ? gameData.whiteUsername() : gameData.blackUsername();
                    manager.broadcastToGame(gameID,
                            new NotificationMessage(playerName + " is in check"));
                }
            }
        } catch (dataaccess.DataAccessException e) {
            // Silently handle database update errors
        }
    }

    private String formatMove(chess.ChessMove move, chess.ChessPiece piece) {
        String from = positionToAlgebraic(move.getStartPosition());
        String to = positionToAlgebraic(move.getEndPosition());
        String pieceName = pieceTypeToName(piece.getPieceType());

        String promotion = "";
        if (move.getPromotionPiece() != null) {
            promotion = " promoting to " + pieceTypeToName(move.getPromotionPiece());
        }

        return pieceName + " from " + from + " to " + to + promotion;
    }

    private String positionToAlgebraic(chess.ChessPosition pos) {
        char col = (char) ('a' + pos.getColumn() - 1);
        return "" + col + pos.getRow();
    }

    private String pieceTypeToName(chess.ChessPiece.PieceType type) {
        return switch (type) {
            case KING -> "King";
            case QUEEN -> "Queen";
            case ROOK -> "Rook";
            case BISHOP -> "Bishop";
            case KNIGHT -> "Knight";
            case PAWN -> "Pawn";
        };
    }

    private void sendError(WsContext ctx, String msg) {
        if (!msg.startsWith("Error")) {
            msg = "Error: " + msg;
        }
        ctx.send(gson.toJson(new ErrorMessage(msg)));
    }
}
