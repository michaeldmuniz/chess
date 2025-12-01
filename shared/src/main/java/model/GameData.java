package model;

import chess.ChessGame;
import chess.ChessPosition;
import chess.ChessMove;
import java.util.Collection;

public record GameData(
        int gameID,
        String whiteUsername,
        String blackUsername,
        String gameName,
        ChessGame game,
        boolean gameOver,
        ChessPosition highlightOrigin,
        Collection<ChessMove> highlightMoves
) {}
