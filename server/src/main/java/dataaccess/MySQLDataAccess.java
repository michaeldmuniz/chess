package dataaccess;

import model.*;
import chess.ChessGame;
import com.google.gson.Gson;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;

public class MySQLDataAccess implements DataAccess {

    private final Gson gson = new Gson();

    public MySQLDataAccess() throws DataAccessException {
        configureDatabase();
    }

    private void configureDatabase() throws DataAccessException {
    }

    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var stmt = conn.prepareStatement("TRUNCATE TABLE auth")) {
                stmt.executeUpdate();
            }
            try (var stmt = conn.prepareStatement("TRUNCATE TABLE game")) {
                stmt.executeUpdate();
            }
            try (var stmt = conn.prepareStatement("TRUNCATE TABLE user")) {
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new DataAccessException("Error clearing database: " + e.getMessage());
        }
    }


    @Override
    public void createUser(UserData user) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        throw new DataAccessException("not implemented");
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        throw new DataAccessException("not implemented");
    }
    public void testConnection() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var stmt = conn.prepareStatement("SELECT 1+1")) {
                var rs = stmt.executeQuery();
                rs.next();
                System.out.println("Database connection test successful! Result: " + rs.getInt(1));
            }
        } catch (Exception e) {
            throw new DataAccessException("Connection test failed: " + e.getMessage());
        }
    }

}
