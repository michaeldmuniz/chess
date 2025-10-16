package dataaccess;

import model.*;
import java.util.*;

public class MemoryDataAccess implements DataAccess {

    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> auths = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();

    private int nextGameID = 1;

    @Override
    public void clear() {
        users.clear();
        auths.clear();
        games.clear();
        nextGameID = 1;
    }

    //  USER METHODS

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (user == null || user.username() == null) {
            throw new DataAccessException("Error: bad request");
        }
        if (users.containsKey(user.username())) {
            throw new DataAccessException("Error: already taken");
        }
        users.put(user.username(), user);
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        var user = users.get(username);
        if (user == null) {
            throw new DataAccessException("Error: user not found");
        }
        return user;
    }

    // AUTH METHODS

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        if (auth == null || auth.authToken() == null) {
            throw new DataAccessException("Error: bad request");
        }
        auths.put(auth.authToken(), auth);
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        var auth = auths.get(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        return auth;
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        if (!auths.containsKey(authToken)) {
            throw new DataAccessException("Error: unauthorized");
        }
        auths.remove(authToken);
    }

    // GAME METHODS

    @Override
    public int createGame(GameData game) throws DataAccessException {
        if (game == null || game.gameName() == null) {
            throw new DataAccessException("Error: bad request");
        }
        int gameID = nextGameID++;
        var newGame = new GameData(gameID,
                game.whiteUsername(),
                game.blackUsername(),
                game.gameName(),
                game.game());
        games.put(gameID, newGame);
        return gameID;
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        var game = games.get(gameID);
        if (game == null) {
            throw new DataAccessException("Error: game not found");
        }
        return game;
    }

    @Override
    public Collection<GameData> listGames() {
        return new ArrayList<>(games.values());
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        if (!games.containsKey(game.gameID())) {
            throw new DataAccessException("Error: game not found");
        }
        games.put(game.gameID(), game);
    }
}
