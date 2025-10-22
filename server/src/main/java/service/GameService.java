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
        // Make sure the user is authorized.
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        // Retrieve all games from the DAO.
        return new ArrayList<>(dao.listGames());
    }

    public void joinGame(String authToken, String playerColor, int gameID) throws DataAccessException {
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        GameData game = dao.getGame(gameID);
        if (game == null) {
            throw new DataAccessException("bad request");
        }

        String username = auth.username();
        if (playerColor == null) {
            throw new DataAccessException("bad request");
        }

        if (playerColor.equalsIgnoreCase("WHITE")) {
            if (game.whiteUsername() != null) {
                throw new DataAccessException("already taken");
            }
            game = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
        } else if (playerColor.equalsIgnoreCase("BLACK")) {
            if (game.blackUsername() != null) {
                throw new DataAccessException("already taken");
            }
            game = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
        } else {
            throw new DataAccessException("bad request");
        }

        dao.updateGame(game);
    }


}
