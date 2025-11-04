package dataaccess;

import model.*;
import chess.ChessGame;
import com.google.gson.Gson;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class MySQLDataAccess implements DataAccess {

    private final Gson gson = new Gson();

    public MySQLDataAccess() throws DataAccessException {
        setupDatabase();
    }


    private void setupDatabase() throws DataAccessException {
        String dbName = DatabaseManager.getDatabaseName();

        // Create database if not already there
        DatabaseManager.createDatabase();

        // Then make tables
        try (var conn = DatabaseManager.getConnection()) {

            try (var userStmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS user (
                    username VARCHAR(50) PRIMARY KEY,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(100) NOT NULL
                )
            """)) {
                userStmt.executeUpdate();
            }

            try (var authStmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS auth (
                    authToken VARCHAR(255) PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    FOREIGN KEY (username) REFERENCES user(username) ON DELETE CASCADE
                )
            """)) {
                authStmt.executeUpdate();
            }

            try (var gameStmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS game (
                    gameID INT AUTO_INCREMENT PRIMARY KEY,
                    whiteUsername VARCHAR(50),
                    blackUsername VARCHAR(50),
                    gameName VARCHAR(100) NOT NULL,
                    gameJSON TEXT NOT NULL,
                    FOREIGN KEY (whiteUsername) REFERENCES user(username) ON DELETE SET NULL,
                    FOREIGN KEY (blackUsername) REFERENCES user(username) ON DELETE SET NULL
                )
            """)) {
                gameStmt.executeUpdate();
            }

            System.out.println("Tables verified for database: " + dbName);

        } catch (SQLException e) {
            throw new DataAccessException("Couldn't initialize tables properly", e);
        }
    }


    @Override
    public void clear() throws DataAccessException {
        String[] tables = {"auth", "game", "user"};

        try (var conn = DatabaseManager.getConnection()) {
            for (String table : tables) {
                try (var stmt = conn.prepareStatement("DELETE FROM " + table)) {
                    stmt.executeUpdate();
                }
            }
            System.out.println(" All tables cleared successfully.");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to clear tables", e);
        }
    }


    @Override
    public void createUser(UserData user) throws DataAccessException {
        final String sql = "INSERT INTO user (username, password, email) VALUES (?, ?, ?)";

        if (user.username() == null || user.password() == null || user.email() == null) {
            throw new DataAccessException("User fields cannot be null");
        }

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.username());
            stmt.setString(2, user.password());
            stmt.setString(3, user.email());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Couldn't insert user (maybe duplicate username?)", e);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        final String sql = "SELECT username, password, email FROM user WHERE username = ?";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email")
                    );
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Couldn't find user", e);
        }
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        final String sql = "INSERT INTO auth (authToken, username) VALUES (?, ?)";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Couldn't create auth token", e);
        }
    }

    @Override
    public AuthData getAuth(String token) throws DataAccessException {
        final String sql = "SELECT authToken, username FROM auth WHERE authToken = ?";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("authToken"),
                            rs.getString("username")
                    );
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Couldn't retrieve auth token", e);
        }
    }

    @Override
    public void deleteAuth(String token) throws DataAccessException {
        final String sql = "DELETE FROM auth WHERE authToken = ?";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Couldn't delete auth", e);
        }
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        final String sql = "INSERT INTO game (whiteUsername, blackUsername, gameName, gameJSON) VALUES (?, ?, ?, ?)";

        String json = gson.toJson(game.game());

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, json);
            stmt.executeUpdate();

            try (var keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new DataAccessException("No game ID returned");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Couldn't create game", e);
        }
    }

    @Override
    public GameData getGame(int id) throws DataAccessException {
        final String sql = "SELECT * FROM game WHERE gameID = ?";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ChessGame gameObj = gson.fromJson(rs.getString("gameJSON"), ChessGame.class);
                    return new GameData(
                            rs.getInt("gameID"),
                            rs.getString("whiteUsername"),
                            rs.getString("blackUsername"),
                            rs.getString("gameName"),
                            gameObj
                    );
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Couldn't get game", e);
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        final String sql = "SELECT * FROM game";
        var games = new ArrayList<GameData>();

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                ChessGame gameObj = gson.fromJson(rs.getString("gameJSON"), ChessGame.class);
                games.add(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        gameObj
                ));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Couldn't list games", e);
        }

        return games;
    }

    @Override
    public void updateGame(GameData updated) throws DataAccessException {
        final String sql = "UPDATE game SET whiteUsername=?, blackUsername=?, gameName=?, gameJSON=? WHERE gameID=?";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, updated.whiteUsername());
            stmt.setString(2, updated.blackUsername());
            stmt.setString(3, updated.gameName());
            stmt.setString(4, gson.toJson(updated.game()));
            stmt.setInt(5, updated.gameID());

            if (stmt.executeUpdate() != 1) {
                throw new DataAccessException("No game updated — check gameID");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Couldn't update game", e);
        }
    }
}
