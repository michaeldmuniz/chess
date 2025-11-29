package client.websocket;

import com.google.gson.Gson;
import jakarta.websocket.*;

import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ErrorMessage;

import java.io.IOException;
import java.net.URI;


@ClientEndpoint
public class WebSocketClient {

    private final Gson gson = new Gson();
    private Session session;

    public interface MessageHandler {
        void onLoadGame(LoadGameMessage msg);
        void onNotification(NotificationMessage msg);
        void onError(ErrorMessage msg);
    }

    private final MessageHandler handler;


    public WebSocketClient(String wsUrl, MessageHandler handler) throws Exception {
        this.handler = handler;
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(this, new URI(wsUrl));
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("[WS] Connected to server");
    }

    @OnMessage
    public void onMessage(String json) {
        try {
            ServerMessage base = gson.fromJson(json, ServerMessage.class);

            switch (base.getServerMessageType()) {
                case LOAD_GAME -> {
                    LoadGameMessage load =
                            gson.fromJson(json, LoadGameMessage.class);
                    System.out.println("[WS] LOAD_GAME received");
                    if (handler != null) {
                        handler.onLoadGame(load);
                    }
                }
                case NOTIFICATION -> {
                    NotificationMessage note =
                            gson.fromJson(json, NotificationMessage.class);
                    System.out.println("[WS] NOTIFICATION: " + note.getMessage());
                    if (handler != null) {
                        handler.onNotification(note);
                    }
                }
                case ERROR -> {
                    ErrorMessage err =
                            gson.fromJson(json, ErrorMessage.class);
                    System.out.println("[WS] ERROR: " + err.getErrorMessage());
                    if (handler != null) {
                        handler.onError(err);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[WS] Failed to parse message: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(CloseReason reason) {
        System.out.println("[WS] Closed: " + reason);
        this.session = null;
    }

    @OnError
    public void onError(Session session, Throwable thr) {
        System.out.println("[WS] Error: " + thr.getMessage());
    }


    public void sendCommand(UserGameCommand cmd) {
        if (session == null || !session.isOpen()) {
            System.out.println("[WS] Cannot send; session is not open");
            return;
        }
        String json = gson.toJson(cmd);
        session.getAsyncRemote().sendText(json);
    }


    public void sendConnect(String authToken, int gameID) {
        UserGameCommand cmd = new UserGameCommand(
                UserGameCommand.CommandType.CONNECT,
                authToken,
                gameID
        );
        sendCommand(cmd);
    }

    public void close() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException ignored) {}
        }
    }
}