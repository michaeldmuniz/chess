package server;

import com.google.gson.Gson;
import io.javalin.websocket.WsContext;
import websocket.messages.ServerMessage;

import java.util.*;

public class WebSocketConnectionManager {

    private final Gson gson = new Gson();

    private final Map<String, WsContext> sessions = new HashMap<>();

    private final Map<String, String> userBySession = new HashMap<>();

    private final Map<String, Integer> gameBySession = new HashMap<>();

    private final Map<String, String> roleBySession = new HashMap<>();

    private final Map<Integer, Set<String>> sessionsInGame = new HashMap<>();


    public void addSession(WsContext ctx, String username, int gameID, String role) {
        String id = ctx.sessionId();

        sessions.put(id, ctx);
        userBySession.put(id, username);
        gameBySession.put(id, gameID);
        roleBySession.put(id, role);

        sessionsInGame.putIfAbsent(gameID, new HashSet<>());
        sessionsInGame.get(gameID).add(id);
    }


    public void removeSession(WsContext ctx) {
        String id = ctx.sessionId();

        Integer gameID = gameBySession.get(id);
        if (gameID != null && sessionsInGame.containsKey(gameID)) {
            sessionsInGame.get(gameID).remove(id);
        }

        sessions.remove(id);
        userBySession.remove(id);
        gameBySession.remove(id);
        roleBySession.remove(id);
    }


    public String getUsername(WsContext ctx) {
        return userBySession.get(ctx.sessionId());
    }

    public Integer getGameId(WsContext ctx) {
        return gameBySession.get(ctx.sessionId());
    }

    public String getRole(WsContext ctx) {
        return roleBySession.get(ctx.sessionId());
    }


    public void sendToSession(String sessionId, ServerMessage msg) {
        System.out.println("[SERVER-DEBUG] sendToSession() called");
        System.out.println("[SERVER-DEBUG] Session ID: " + sessionId);
        System.out.println("[SERVER-DEBUG] Message type: " + (msg != null ? msg.getServerMessageType() : "null"));
        WsContext ctx = sessions.get(sessionId);
        System.out.println("[SERVER-DEBUG] Context found: " + (ctx != null));
        System.out.println("[SERVER-DEBUG] Session isOpen: " + (ctx != null && ctx.session.isOpen()));
        if (ctx != null && ctx.session.isOpen()) {
            String json = gson.toJson(msg);
            System.out.println("[SERVER-DEBUG] Sending JSON: " + json);
            System.out.println("[SERVER-DEBUG] JSON length: " + json.length());
            ctx.send(json);
            System.out.println("[SERVER-DEBUG] send() completed");
        } else {
            System.out.println("[SERVER-DEBUG] Cannot send - context null or session closed");
        }
    }

    public void broadcastToGame(int gameID, ServerMessage msg) {
        if (!sessionsInGame.containsKey(gameID)) return;

        String json = gson.toJson(msg);

        for (String sessionId : sessionsInGame.get(gameID)) {
            WsContext ctx = sessions.get(sessionId);
            if (ctx != null && ctx.session.isOpen()) {
                ctx.send(json);
            }
        }
    }

    public void broadcastToGameExcept(WsContext excludedCtx, int gameID, ServerMessage msg) {
        System.out.println("[SERVER-DEBUG] broadcastToGameExcept() called");
        System.out.println("[SERVER-DEBUG] Game ID: " + gameID);
        System.out.println("[SERVER-DEBUG] Excluded session: " + excludedCtx.sessionId());
        System.out.println("[SERVER-DEBUG] Message type: " + (msg != null ? msg.getServerMessageType() : "null"));
        if (!sessionsInGame.containsKey(gameID)) {
            System.out.println("[SERVER-DEBUG] No sessions in game");
            return;
        }

        String excludedId = excludedCtx.sessionId();
        String json = gson.toJson(msg);
        System.out.println("[SERVER-DEBUG] Broadcast JSON: " + json);
        System.out.println("[SERVER-DEBUG] Sessions in game: " + sessionsInGame.get(gameID).size());

        for (String sessionId : sessionsInGame.get(gameID)) {
            if (!sessionId.equals(excludedId)) {
                System.out.println("[SERVER-DEBUG] Broadcasting to session: " + sessionId);
                WsContext ctx = sessions.get(sessionId);
                if (ctx != null && ctx.session.isOpen()) {
                    ctx.send(json);
                    System.out.println("[SERVER-DEBUG] Sent to session: " + sessionId);
                } else {
                    System.out.println("[SERVER-DEBUG] Cannot send to session " + sessionId + " - context null or closed");
                }
            }
        }
    }
}
