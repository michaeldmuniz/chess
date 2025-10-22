package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import model.GameData;
import model.AuthData;
import dataaccess.DataAccessException;
import java.util.ArrayList;
import java.util.List;


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
    public List<GameData> listGames(String authToken) throws DataAccessException {
        // 1. Make sure the user is authorized.
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        // 2. Retrieve all games from the DAO.
        return new ArrayList<>(dao.listGames());
    }

}
