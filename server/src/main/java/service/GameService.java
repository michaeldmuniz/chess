package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;

public class GameService {

    private final DataAccess dao;

    public GameService(DataAccess dao) {
        this.dao = dao;
    }

    public int createGame(String authToken, String gameName) throws DataAccessException {
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        if (gameName == null || gameName.isBlank()) {
            throw new DataAccessException("bad request");
        }

        ChessGame newGame = new ChessGame();
        GameData gameData = new GameData(
                0,
                null,
                null,
                gameName,
                newGame
        );

        return dao.createGame(gameData);
    }
}
