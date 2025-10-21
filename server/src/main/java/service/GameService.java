package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import chess.ChessGame;

import java.util.Collection;

public class GameService {

    private final DataAccess dao;

    public GameService(DataAccess dao) {
        this.dao = dao;
    }

    public GameData createGame(String authToken, String gameName) throws DataAccessException {
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        ChessGame game = new ChessGame();

        GameData newGame = new GameData(
                0,               // temp ID, real one assigned in DAO
                null,            // whiteUsername
                null,            // blackUsername
                gameName,
                game
        );

        int gameID = dao.createGame(newGame);

        return dao.getGame(gameID);
    }


    public Collection<GameData> listGames(String authToken) throws DataAccessException {
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        return dao.listGames();
    }
}
