package server;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import websocket.commands.UserGameCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler {

    private final Gson gson = new Gson();

    private final Map<String, WsContext> connections = new ConcurrentHashMap<>();

    public void register(Javalin app) {
        app.ws("/ws", ws -> {

            ws.onConnect(ctx -> {
                connections.put(ctx.sessionId(), ctx);
                System.out.println("[WS] Connected: " + ctx.sessionId());
            });

            ws.onClose(ctx -> {
                connections.remove(ctx.sessionId());
                System.out.println("[WS] Closed: " + ctx.sessionId());
            });

            ws.onError(ctx -> {
                System.out.println("[WS] Error on " + ctx.sessionId() + ": " + ctx.error());
            });

            ws.onMessage(ctx -> {
                String raw = ctx.message();
                System.out.println("[WS] Raw message: " + raw);

                try {
                    UserGameCommand cmd = gson.fromJson(raw, UserGameCommand.class);

                    System.out.printf(
                            "[WS] Parsed command: type=%s gameID=%s auth=%s%n",
                            cmd.getCommandType(), cmd.getGameID(), cmd.getAuthToken()
                    );

                    // TODO: Implement actual command handling later.

                } catch (Exception ex) {
                    System.out.println("[WS] Failed to parse UserGameCommand: " + ex.getMessage());
                }
            });
        });
    }
}
