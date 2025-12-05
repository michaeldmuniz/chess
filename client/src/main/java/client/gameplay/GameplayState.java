package client.gameplay;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;

import java.util.Collection;
import java.util.List;

public class GameplayState {

    private ChessGame currentGame;
    private final boolean whitePerspective;
    private boolean running = true;

    private ChessPosition highlightOrigin = null;
    private Collection<ChessMove> highlightMoves = List.of();

    private boolean redrawRequested = false;
    private boolean gameLoaded = false;


    public GameplayState(boolean whitePerspective) {
        this.whitePerspective = whitePerspective;
    }


    public synchronized void setGame(ChessGame game) {
        this.currentGame = game;
        markRedraw();                // board should update when game changes
    }

    public synchronized boolean isGameLoaded() {
        return gameLoaded;
    }

    public synchronized void setGameLoaded(boolean loaded) {
        this.gameLoaded = loaded;
    }

    public synchronized ChessGame getGame() {
        return currentGame;
    }

    public boolean isWhitePerspective() {
        return whitePerspective;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void stop() {
        running = false;
    }

    public synchronized ChessPosition getHighlightOrigin() {
        return highlightOrigin;
    }

    public synchronized Collection<ChessMove> getHighlightMoves() {
        return highlightMoves;
    }

    public synchronized void setHighlight(ChessPosition origin, Collection<ChessMove> moves) {
        this.highlightOrigin = origin;
        this.highlightMoves = (moves != null ? moves : List.of());
        markRedraw();
    }




    public synchronized boolean shouldRedraw() {
        return redrawRequested;
    }

    public synchronized void markRedraw() {
        this.redrawRequested = true;
    }

    public synchronized void clearRedraw() {
        this.redrawRequested = false;
    }
}
