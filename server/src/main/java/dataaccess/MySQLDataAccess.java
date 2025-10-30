package dataaccess;

import model.*;
import chess.ChessGame;
import com.google.gson.Gson;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


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
        var sql = "INSERT INTO user (username, password, email) VALUES (?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            conn.setCatalog("chess");
            stmt.setString(1, user.username());
            stmt.setString(2, user.password());
            stmt.setString(3, user.email());
            stmt.executeUpdate();

        } catch (Exception e) {
            throw new DataAccessException("Error creating user: " + e.getMessage());
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        var sql = "SELECT username, password, email FROM user WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            conn.setCatalog("chess");
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email")
                    );
                }
                return null;
            }

        } catch (Exception e) {
            throw new DataAccessException("Error retrieving user: " + e.getMessage());
        }
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        var sql = "INSERT INTO auth (authToken, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            conn.setCatalog("chess");
            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());
            stmt.executeUpdate();

        } catch (Exception e) {
            throw new DataAccessException("Error creating auth: " + e.getMessage());
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        var sql = "SELECT authToken, username FROM auth WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            conn.setCatalog("chess");
            stmt.setString(1, authToken);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("authToken"),
                            rs.getString("username")
                    );
                }
                return null;
            }

        } catch (Exception e) {
            throw new DataAccessException("Error retrieving auth: " + e.getMessage());
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        var sql = "DELETE FROM auth WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            conn.setCatalog("chess");
            stmt.setString(1, authToken);
            stmt.executeUpdate();

        } catch (Exception e) {
            throw new DataAccessException("Error deleting auth: " + e.getMessage());
        }
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        var sql = "INSERT INTO game (whiteUsername, blackUsername, gameName, gameJSON) VALUES (?, ?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            conn.setCatalog("chess");

            String gameJson = gson.toJson(game.game()); // Serialize ChessGame object

            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJson);

            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // Return the auto-generated gameID
                }
            }

            throw new DataAccessException("Game ID not generated");

        } catch (Exception e) {
            throw new DataAccessException("Error creating game: " + e.getMessage());
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        var sql = "SELECT gameID, whiteUsername, blackUsername, gameName, gameJSON FROM game WHERE gameID = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            conn.setCatalog("chess");
            stmt.setInt(1, gameID);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String gameJson = rs.getString("gameJSON");
                    ChessGame game = gson.fromJson(gameJson, ChessGame.class);

                    return new GameData(
                            rs.getInt("gameID"),
                            rs.getString("whiteUsername"),
                            rs.getString("blackUsername"),
                            rs.getString("gameName"),
                            game
                    );
                }
            }

            return null; // Game not found

        } catch (Exception e) {
            throw new DataAccessException("Error retrieving game: " + e.getMessage());
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        var games = new java.util.ArrayList<GameData>();
        var sql = "SELECT gameID, whiteUsername, blackUsername, gameName, gameJSON FROM game";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            conn.setCatalog("chess");

            while (rs.next()) {
                String gameJson = rs.getString("gameJSON");
                ChessGame game = gson.fromJson(gameJson, ChessGame.class);

                games.add(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        game
                ));
            }

            return games;

        } catch (Exception e) {
            throw new DataAccessException("Error listing games: " + e.getMessage());
        }
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        var sql = "UPDATE game SET whiteUsername = ?, blackUsername = ?, gameName = ?, gameJSON = ? WHERE gameID = ?";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            conn.setCatalog("chess");

            String gameJson = gson.toJson(game.game());

            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJson);
            stmt.setInt(5, game.gameID());

            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new DataAccessException("Game not found");
            }

        } catch (Exception e) {
            throw new DataAccessException("Error updating game: " + e.getMessage());
        }
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
