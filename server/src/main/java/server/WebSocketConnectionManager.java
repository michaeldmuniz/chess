package server;

import com.google.gson.Gson;
import io.javalin.websocket.WsContext;
import websocket.messages.ServerMessage;

import java.util.*;

public class WebSocketConnectionManager {

    private final Gson gson = new Gson();

    // sessionId WsContext
    private final Map<String, WsContext> sessions = new HashMap<>();

    // sessionId username
    private final Map<String, String> userBySession = new HashMap<>();

    // sessionId gameID
    private final Map<String, Integer> gameBySession = new HashMap<>();

    // gameID all sessionIds in that game
    private final Map<Integer, Set<String>> sessionsInGame = new HashMap<>();


    public void addSession(WsContext ctx, String username, int gameID) {
        String id = ctx.sessionId();

        sessions.put(id, ctx);
        userBySession.put(id, username);
        gameBySession.put(id, gameID);

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
    }


    public String getUsername(WsContext ctx) {
        return userBySession.get(ctx.sessionId());
    }

    public Integer getGameId(WsContext ctx) {
        return gameBySession.get(ctx.sessionId());
    }


    public void sendToSession(String sessionId, ServerMessage msg) {
        WsContext ctx = sessions.get(sessionId);
        if (ctx != null && ctx.session.isOpen()) {
            ctx.send(gson.toJson(msg));
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

    public void broadcastToGameExcept(int gameID, String excludedSessionId, ServerMessage msg) {
        if (!sessionsInGame.containsKey(gameID)) return;

        String json = gson.toJson(msg);

        for (String sessionId : sessionsInGame.get(gameID)) {
            if (!sessionId.equals(excludedSessionId)) {
                WsContext ctx = sessions.get(sessionId);
                if (ctx != null && ctx.session.isOpen()) {
                    ctx.send(json);
                }
            }
        }
    }
}
