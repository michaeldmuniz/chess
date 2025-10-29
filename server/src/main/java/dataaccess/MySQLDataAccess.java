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
        try (var conn = DatabaseManager.getConnection()) {
            try (var stmt = conn.prepareStatement("CREATE DATABASE IF NOT EXISTS chess")) {
                stmt.executeUpdate();
            }

            conn.setCatalog("chess");

            try (var stmt = conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS user (
                username VARCHAR(50) PRIMARY KEY,
                password VARCHAR(255) NOT NULL,
                email VARCHAR(100) NOT NULL
            )
        """)) {
                stmt.executeUpdate();
            }

            try (var stmt = conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS auth (
                authToken VARCHAR(255) PRIMARY KEY,
                username VARCHAR(50) NOT NULL,
                FOREIGN KEY (username) REFERENCES user(username) ON DELETE CASCADE
            )
        """)) {
                stmt.executeUpdate();
            }

            try (var stmt = conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS game (
                gameID INT AUTO_INCREMENT PRIMARY KEY,
                whiteUsername VARCHAR(50),
                blackUsername VARCHAR(50),
                gameName VARCHAR(100) NOT NULL,
                gameJson TEXT NOT NULL,
                FOREIGN KEY (whiteUsername) REFERENCES user(username) ON DELETE SET NULL,
                FOREIGN KEY (blackUsername) REFERENCES user(username) ON DELETE SET NULL
            )
        """)) {
                stmt.executeUpdate();
            }

            System.out.println("Database and tables verified/created successfully.");

        } catch (Exception e) {
            throw new DataAccessException("Error configuring database: " + e.getMessage());
        }
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
