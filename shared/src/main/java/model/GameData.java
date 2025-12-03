package model;

import chess.ChessGame;

public record GameData(
        int gameID,
        String whiteUsername,
        String blackUsername,
        String gameName,
        ChessGame game,
        boolean gameOver
) {
    public GameData(int gameID, String whiteUsername, String blackUsername,
                    String gameName, ChessGame game) {
        this(gameID, whiteUsername, blackUsername, gameName, game, false);
    }
}
