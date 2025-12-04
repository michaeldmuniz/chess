package client.websocket;

import com.google.gson.Gson;
import jakarta.websocket.*;

import client.gameplay.GameplayState;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ErrorMessage;

import java.io.IOException;
import java.net.URI;

public class WebSocketClient extends Endpoint {

    private final Gson gson = new Gson();
    private Session session;
    private final String wsUrl;

    private GameplayState gameplayState;

    public interface MessageHandler {
        void onLoadGame(LoadGameMessage msg);
        void onNotification(NotificationMessage msg);
        void onError(ErrorMessage msg);
    }

    private final MessageHandler handler;

    public WebSocketClient(String wsUrl, MessageHandler handler) {
        this.wsUrl = wsUrl;
        this.handler = handler;
    }

    public void attachState(GameplayState state) {
        this.gameplayState = state;
    }

    public void connect() throws Exception {
        System.out.println("[WS-DEBUG] connect() called, URL: " + wsUrl);
        System.out.println("[WS-DEBUG] pendingAuth before connect: " + (pendingAuth != null ? "exists (gameID=" + pendingAuth.gameID + ")" : "null"));

        // Disable Tyrus extensions via system property to prevent opcode 7 issues
        System.setProperty("org.glassfish.tyrus.client.disableExtensions", "true");

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        // Configure client properties to avoid protocol issues
        container.setDefaultMaxSessionIdleTimeout(60000);
        container.setDefaultMaxTextMessageBufferSize(65536);

        System.out.println("[WS-DEBUG] Container default max idle timeout: " + container.getDefaultMaxSessionIdleTimeout());
        System.out.println("[WS-DEBUG] Container default max text buffer: " + container.getDefaultMaxTextMessageBufferSize());
        System.out.println("[WS-DEBUG] Connecting to server...");

        // Create a minimal ClientEndpointConfig with no extensions to avoid protocol issues
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                .extensions(java.util.Collections.emptyList())  // Disable all extensions
                .preferredSubprotocols(java.util.Collections.emptyList())  // No subprotocols
                .configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(java.util.Map<String, java.util.List<String>> headers) {
                        // Remove any extension-related headers
                        headers.remove("Sec-WebSocket-Extensions");
                    }
                })
                .build();

        container.connectToServer(this, config, new URI(wsUrl));
        System.out.println("[WS-DEBUG] connect() returned");
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        System.out.println("[WS-DEBUG] onOpen() called");
        System.out.println("[WS-DEBUG] Session ID: " + session.getId());
        System.out.println("[WS-DEBUG] Session isOpen: " + session.isOpen());
        this.session = session;
        System.out.println("[WS] Connected to server");

        // Register message handler as shown in documentation
        session.addMessageHandler(new jakarta.websocket.MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String json) {
                handleMessage(json);
            }
        });

        System.out.println("[WS-DEBUG] pendingAuth in onOpen: " + (pendingAuth != null ? "exists (gameID=" + pendingAuth.gameID + ", authToken=" + (pendingAuth.authToken != null ? "present" : "null") + ")" : "null"));
        if (pendingAuth != null) {
            // Wait longer to ensure Tyrus has finished any initialization frames
            // The opcode 7 error suggests Tyrus sends something right after connection
            new Thread(() -> {
                try {
                    // Wait longer to let any Tyrus initialization complete
                    Thread.sleep(500); // Increased delay
                    if (session != null && session.isOpen() && pendingAuth != null) {
                        UserGameCommand cmd = new UserGameCommand(
                                UserGameCommand.CommandType.CONNECT,
                                pendingAuth.authToken,
                                pendingAuth.gameID
                        );
                        System.out.println("[WS-DEBUG] Sending CONNECT command from onOpen (delayed), gameID: " + pendingAuth.gameID);
                        sendCommand(cmd);
                        pendingAuth = null; // Clear after sending
                    } else {
                        System.out.println("[WS-DEBUG] Session closed before sending CONNECT");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("[WS-DEBUG] Thread interrupted");
                } catch (Exception e) {
                    System.out.println("[WS-DEBUG] Exception sending CONNECT: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        } else {
            System.out.println("[WS-DEBUG] No pendingAuth, not sending CONNECT from onOpen");
        }
    }

    private void handleMessage(String json) {
        System.out.println("[WS-DEBUG] onMessage() called");
        System.out.println("[WS-DEBUG] Raw JSON received: " + json);
        System.out.println("[WS-DEBUG] JSON length: " + (json != null ? json.length() : 0));
        try {
            ServerMessage base = gson.fromJson(json, ServerMessage.class);
            System.out.println("[WS-DEBUG] Parsed ServerMessage: " + (base != null ? base.getClass().getSimpleName() : "null"));

            if (base == null || base.getServerMessageType() == null) {
                System.out.println("[WS] Received message with null type: " + json);
                return;
            }

            System.out.println("[WS-DEBUG] Message type: " + base.getServerMessageType());
            switch (base.getServerMessageType()) {

                case LOAD_GAME -> {
                    LoadGameMessage load = gson.fromJson(json, LoadGameMessage.class);
                    System.out.println("[WS] LOAD_GAME received");

                    if (gameplayState != null) {
                        gameplayState.setGame(load.getGame());
                    }

                    if (handler != null) {
                        handler.onLoadGame(load);
                    }
                }

                case NOTIFICATION -> {
                    NotificationMessage note = gson.fromJson(json, NotificationMessage.class);
                    System.out.println("[WS] NOTIFICATION: " + note.getMessage());

                    if (handler != null) {
                        handler.onNotification(note);
                    }
                }

                case ERROR -> {
                    ErrorMessage err = gson.fromJson(json, ErrorMessage.class);
                    System.out.println("[WS] ERROR: " + err.getErrorMessage());

                    if (handler != null) {
                        handler.onError(err);
                    }
                }

                default -> {
                    System.out.println("[WS] Unknown message type: " + base.getServerMessageType());
                }
            }

        } catch (Exception e) {
            System.out.println("[WS] Failed to parse message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Note: onClose and onError are handled by the Session's CloseHandler and ErrorHandler
    // We can add listeners if needed, but the Endpoint base class handles these

    public void sendCommand(UserGameCommand cmd) {
        System.out.println("[WS-DEBUG] sendCommand() called");
        System.out.println("[WS-DEBUG] Command type: " + (cmd != null ? cmd.getCommandType() : "null"));
        System.out.println("[WS-DEBUG] Session: " + (session != null ? "exists" : "null"));
        System.out.println("[WS-DEBUG] Session isOpen: " + (session != null ? session.isOpen() : "N/A"));

        if (session == null || !session.isOpen()) {
            System.out.println("[WS] Cannot send; session not open");
            return;
        }
        try {
            String json = gson.toJson(cmd);
            System.out.println("[WS-DEBUG] Sending JSON: " + json);
            System.out.println("[WS-DEBUG] Using getBasicRemote().sendText()");
            session.getBasicRemote().sendText(json);
            System.out.println("[WS-DEBUG] sendText() completed successfully");
        } catch (IOException e) {
            System.out.println("[WS-DEBUG] IOException in sendCommand: " + e.getMessage());
            System.out.println("[WS] Failed to send command: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("[WS-DEBUG] Unexpected exception in sendCommand: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendConnect(String authToken, int gameID) {
        System.out.println("[WS-DEBUG] sendConnect() called");
        System.out.println("[WS-DEBUG] gameID: " + gameID);
        System.out.println("[WS-DEBUG] authToken: " + (authToken != null ? "present (length=" + authToken.length() + ")" : "null"));

        pendingAuth = new PendingConnect();
        pendingAuth.authToken = authToken;
        pendingAuth.gameID = gameID;
        System.out.println("[WS-DEBUG] Set pendingAuth with gameID: " + gameID);

        System.out.println("[WS-DEBUG] Checking if session is ready...");
        System.out.println("[WS-DEBUG] session: " + (session != null ? "exists" : "null"));
        System.out.println("[WS-DEBUG] session.isOpen: " + (session != null ? session.isOpen() : "N/A"));

        if (session != null && session.isOpen()) {
            System.out.println("[WS-DEBUG] Session is open, sending CONNECT immediately");
            UserGameCommand cmd = new UserGameCommand(
                    UserGameCommand.CommandType.CONNECT,
                    authToken,
                    gameID
            );
            sendCommand(cmd);
        } else {
            System.out.println("[WS-DEBUG] Session not ready, will send CONNECT from onOpen()");
        }
    }

    public void close() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException ignore) {}
        }
    }

    private static class PendingConnect {
        String authToken;
        int gameID;
    }
    private PendingConnect pendingAuth;

}
