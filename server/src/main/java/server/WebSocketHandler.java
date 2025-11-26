package server;

import com.google.gson.Gson;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import websocket.messages.ServerMessage;
import websocket.messages.ServerLoadGameMessage;
import websocket.messages.ServerErrorMessage;
import websocket.messages.ServerNotificationMessage;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import chess.ChessGame;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint(value = "/ws")
public class WebSocketHandler {

    private static final Gson gson = new Gson();
    private static DataAccess dao;
    private static final ConnectionManager connections = new ConnectionManager();

    // Set by Server during initialization
    public static void setDataAccess(DataAccess dataAccess) {
        dao = dataAccess;
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("WebSocket opened: " + session.getId());
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        System.out.println("Received WebSocket message: " + message);

        try {
            UserGameCommand cmd = gson.fromJson(message, UserGameCommand.class);

            switch (cmd.getCommandType()) {
                case CONNECT -> handleConnect(session, cmd);
                case MAKE_MOVE -> {
                    System.out.println("MAKE_MOVE command received (no logic yet)");
                }
                case LEAVE -> handleLeave(session, cmd);
                case RESIGN -> {
                    System.out.println("RESIGN command received (no logic yet)");
                }
            }

        } catch (Exception ex) {
            System.out.println("Failed to parse message: " + ex.getMessage());
            sendError(session, "Error: " + ex.getMessage());
        }
    }

    private void handleConnect(Session session, UserGameCommand cmd) {
        try {
            // Validate auth token
            if (cmd.getAuthToken() == null) {
                sendError(session, "Error: unauthorized");
                return;
            }

            AuthData auth = dao.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(session, "Error: unauthorized");
                return;
            }

            // Validate game ID
            if (cmd.getGameID() == null) {
                sendError(session, "Error: bad request");
                return;
            }

            GameData game = dao.getGame(cmd.getGameID());
            if (game == null) {
                sendError(session, "Error: bad request");
                return;
            }

            // Add session to connection manager
            connections.add(cmd.getGameID(), session);

            // Send LOAD_GAME to root client
            ServerLoadGameMessage loadMessage = new ServerLoadGameMessage(game.game());
            sendMessage(session, loadMessage);

            // Determine if user is player or observer
            String username = auth.username();
            boolean isPlayer = username.equals(game.whiteUsername()) || username.equals(game.blackUsername());
            String playerColor = null;
            if (username.equals(game.whiteUsername())) {
                playerColor = "white";
            } else if (username.equals(game.blackUsername())) {
                playerColor = "black";
            }

            // Send notification to other clients
            String notificationMsg;
            if (isPlayer && playerColor != null) {
                notificationMsg = username + " joined the game as " + playerColor;
            } else {
                notificationMsg = username + " joined the game as an observer";
            }

            ServerNotificationMessage notification = new ServerNotificationMessage(notificationMsg);
            broadcastToOthers(cmd.getGameID(), session, notification);

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        } catch (Exception e) {
            sendError(session, "Error: unexpected failure");
        }
    }

    private void sendMessage(Session session, ServerMessage message) {
        try {
            String json = gson.toJson(message);
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }

    private void sendError(Session session, String errorMessage) {
        ServerErrorMessage error = new ServerErrorMessage(errorMessage);
        sendMessage(session, error);
    }

    private void handleLeave(Session session, UserGameCommand cmd) {
        try {
            // Validate auth token
            if (cmd.getAuthToken() == null) {
                sendError(session, "Error: unauthorized");
                return;
            }

            AuthData auth = dao.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(session, "Error: unauthorized");
                return;
            }

            // Validate game ID
            if (cmd.getGameID() == null) {
                sendError(session, "Error: bad request");
                return;
            }

            GameData game = dao.getGame(cmd.getGameID());
            if (game == null) {
                sendError(session, "Error: bad request");
                return;
            }

            String username = auth.username();

            // If user is a player, update the game to remove them
            GameData updatedGame = game;
            if (username.equals(game.whiteUsername())) {
                updatedGame = new GameData(game.gameID(), null, game.blackUsername(), game.gameName(), game.game());
                dao.updateGame(updatedGame);
            } else if (username.equals(game.blackUsername())) {
                updatedGame = new GameData(game.gameID(), game.whiteUsername(), null, game.gameName(), game.game());
                dao.updateGame(updatedGame);
            }

            // Remove session from connection manager
            connections.remove(session);

            // Send notification to other clients
            String notificationMsg = username + " left the game";
            ServerNotificationMessage notification = new ServerNotificationMessage(notificationMsg);
            broadcastToOthers(cmd.getGameID(), session, notification);

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        } catch (Exception e) {
            sendError(session, "Error: unexpected failure");
        }
    }

    private void broadcastToOthers(int gameID, Session excludeSession, ServerMessage message) {
        var otherSessions = connections.getOtherSessions(gameID, excludeSession);
        for (Session session : otherSessions) {
            sendMessage(session, message);
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("WebSocket closed: " + session.getId());
        connections.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("WebSocket error: " + error.getMessage());
    }
}
