package client.gameplay;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;

import java.util.Collection;
import java.util.List;

public class GameplayState {

    private ChessGame currentGame;
    private boolean running = true;
    private final boolean whitePerspective;

    public GameplayState(boolean whitePerspective) {
        this.whitePerspective = whitePerspective;
    }

    public synchronized void setGame(ChessGame game) {
        this.currentGame = game;
    }

    public synchronized ChessGame getGame() {
        return currentGame;
    }

    public boolean isWhitePerspective() {
        return whitePerspective;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }
    private ChessPosition highlightOrigin = null;
    private Collection<ChessMove> highlightMoves = List.of();

}
