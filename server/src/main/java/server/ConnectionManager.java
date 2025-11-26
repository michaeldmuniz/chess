package server;

import jakarta.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Manages WebSocket connections for games.
 * Tracks which sessions are connected to which games.
 */
public class ConnectionManager {
    
    // Map from gameID to set of sessions connected to that game
    private final ConcurrentHashMap<Integer, Set<Session>> gameConnections = new ConcurrentHashMap<>();
    
    // Map from session to gameID (for quick lookup when session closes)
    private final ConcurrentHashMap<Session, Integer> sessionToGame = new ConcurrentHashMap<>();
    
    /**
     * Add a session to a game
     */
    public void add(int gameID, Session session) {
        gameConnections.computeIfAbsent(gameID, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToGame.put(session, gameID);
    }
    
    /**
     * Remove a session from a game
     */
    public void remove(Session session) {
        Integer gameID = sessionToGame.remove(session);
        if (gameID != null) {
            Set<Session> sessions = gameConnections.get(gameID);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    gameConnections.remove(gameID);
                }
            }
        }
    }
    
    /**
     * Get all sessions connected to a game
     */
    public Set<Session> getSessions(int gameID) {
        return new HashSet<>(gameConnections.getOrDefault(gameID, Set.of()));
    }
    
    /**
     * Get all sessions connected to a game except the specified session
     */
    public Set<Session> getOtherSessions(int gameID, Session excludeSession) {
        Set<Session> allSessions = getSessions(gameID);
        allSessions.remove(excludeSession);
        return allSessions;
    }
}

