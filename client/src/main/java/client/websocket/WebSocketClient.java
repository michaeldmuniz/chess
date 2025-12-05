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
        // Disable Tyrus extensions via system property to prevent opcode 7 issues
        System.setProperty("org.glassfish.tyrus.client.disableExtensions", "true");

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        // Configure client properties to avoid protocol issues
        container.setDefaultMaxSessionIdleTimeout(60000);
        container.setDefaultMaxTextMessageBufferSize(65536);

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
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.session = session;
        System.out.println("Connected to server");

        // Register message handler as shown in documentation
        session.addMessageHandler(new jakarta.websocket.MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String json) {
                handleMessage(json);
            }
        });

        if (pendingAuth != null) {
            // Wait to ensure Tyrus has finished any initialization frames
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    if (session != null && session.isOpen() && pendingAuth != null) {
                        UserGameCommand cmd = new UserGameCommand(
                                UserGameCommand.CommandType.CONNECT,
                                pendingAuth.authToken,
                                pendingAuth.gameID
                        );
                        sendCommand(cmd);
                        pendingAuth = null; // Clear after sending
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // Silently handle exceptions
                }
            }).start();
        }
    }

    private void handleMessage(String json) {
        try {
            ServerMessage base = gson.fromJson(json, ServerMessage.class);

            if (base == null || base.getServerMessageType() == null) {
                return;
            }

            switch (base.getServerMessageType()) {

                case LOAD_GAME -> {
                    LoadGameMessage load = gson.fromJson(json, LoadGameMessage.class);

                    if (gameplayState != null) {
                        gameplayState.setGame(load.getGame());
                    }

                    if (handler != null) {
                        handler.onLoadGame(load);
                    }
                }

                case NOTIFICATION -> {
                    NotificationMessage note = gson.fromJson(json, NotificationMessage.class);

                    if (handler != null) {
                        handler.onNotification(note);
                    }
                }

                case ERROR -> {
                    ErrorMessage err = gson.fromJson(json, ErrorMessage.class);

                    if (handler != null) {
                        handler.onError(err);
                    }
                }
            }

        } catch (Exception e) {
            // Silently handle parsing errors
        }
    }


    public void sendCommand(UserGameCommand cmd) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = gson.toJson(cmd);
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            // Silently handle send errors
        } catch (Exception e) {
            // Silently handle send errors
        }
    }

    public void sendConnect(String authToken, int gameID) {
        pendingAuth = new PendingConnect();
        pendingAuth.authToken = authToken;
        pendingAuth.gameID = gameID;

        if (session != null && session.isOpen()) {
            UserGameCommand cmd = new UserGameCommand(
                    UserGameCommand.CommandType.CONNECT,
                    authToken,
                    gameID
            );
            sendCommand(cmd);
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
