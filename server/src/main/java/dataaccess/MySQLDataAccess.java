package dataaccess;

import model.*;
import chess.ChessGame;
import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.SQLException;
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
        final String dbName = "chess";

        final String[] schemaDefinitions = {
                "CREATE DATABASE IF NOT EXISTS " + dbName,
                """
        CREATE TABLE IF NOT EXISTS user (
            username VARCHAR(50) PRIMARY KEY,
            password VARCHAR(255) NOT NULL,
            email VARCHAR(100) NOT NULL
        )
        """,
                """
        CREATE TABLE IF NOT EXISTS auth (
            authToken VARCHAR(255) PRIMARY KEY,
            username VARCHAR(50) NOT NULL,
            FOREIGN KEY (username) REFERENCES user(username) ON DELETE CASCADE
        )
        """,
                """
        CREATE TABLE IF NOT EXISTS game (
            gameID INT AUTO_INCREMENT PRIMARY KEY,
            whiteUsername VARCHAR(50),
            blackUsername VARCHAR(50),
            gameName VARCHAR(100) NOT NULL,
            gameJSON TEXT NOT NULL
        )
        """
        };

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement dbStmt = conn.prepareStatement(schemaDefinitions[0])) {
                dbStmt.execute();
            }

            conn.setCatalog(dbName);

            for (int i = 1; i < schemaDefinitions.length; i++) {
                try (PreparedStatement stmt = conn.prepareStatement(schemaDefinitions[i])) {
                    stmt.execute();
                }
            }

            System.out.println("[Init] Database and tables initialized.");

        } catch (SQLException se) {
            throw new DataAccessException("Failed to set up database schema", se);
        }
    }


    @Override
    public void clear() throws DataAccessException {
        final String[] tables = { "auth", "game", "user" };

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setCatalog("chess");

            for (String table : tables) {
                String sql = "DELETE FROM " + table;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Database clearing operation failed", e);
        }
    }


    @Override
    public void createUser(UserData newUser) throws DataAccessException {
        final String INSERT_USER_SQL =
                "INSERT INTO user (username, password, email) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setCatalog("chess");

            try (PreparedStatement ps = connection.prepareStatement(INSERT_USER_SQL)) {
                // Parameter binding in a slightly different order, with validation
                String uname = newUser.username();
                String pwd = newUser.password();
                String mail = newUser.email();

                if (uname == null || pwd == null || mail == null) {
                    throw new DataAccessException("User fields cannot be null");
                }

                ps.setString(1, uname);
                ps.setString(2, pwd);
                ps.setString(3, mail);

                int affected = ps.executeUpdate();
                if (affected != 1) {
                    throw new DataAccessException("Unexpected number of rows inserted: " + affected);
                }
            }

        } catch (SQLException sqlEx) {
            throw new DataAccessException("Failed to insert new user into MySQL database", sqlEx);
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
    public int createGame(GameData newGame) throws DataAccessException {
        final String INSERT_GAME_SQL =
                "INSERT INTO game (whiteUsername, blackUsername, gameName, gameJSON) VALUES (?, ?, ?, ?)";

        ChessGame coreGame = newGame.game();
        String serializedGame = gson.toJson(coreGame);

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setCatalog("chess");

            try (PreparedStatement statement = connection.prepareStatement(
                    INSERT_GAME_SQL, PreparedStatement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, newGame.whiteUsername());
                statement.setString(2, newGame.blackUsername());
                statement.setString(3, newGame.gameName());
                statement.setString(4, serializedGame);

                int result = statement.executeUpdate();
                if (result == 0) {
                    throw new DataAccessException("No game was inserted.");
                }

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    } else {
                        throw new DataAccessException("Game ID not returned by database.");
                    }
                }

            }

        } catch (SQLException sqlEx) {
            throw new DataAccessException("Failed to insert game into database", sqlEx);
        }
    }

    @Override
    public GameData getGame(int id) throws DataAccessException {
        final String FETCH_SQL =
                "SELECT gameID, whiteUsername, blackUsername, gameName, gameJSON FROM game WHERE gameID = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setCatalog("chess");

            try (PreparedStatement query = conn.prepareStatement(FETCH_SQL)) {
                query.setInt(1, id);

                try (ResultSet result = query.executeQuery()) {
                    if (!result.next()) {
                        return null;
                    }

                    ChessGame parsedGame = gson.fromJson(result.getString("gameJSON"), ChessGame.class);

                    return new GameData(
                            result.getInt("gameID"),
                            result.getString("whiteUsername"),
                            result.getString("blackUsername"),
                            result.getString("gameName"),
                            parsedGame
                    );
                }
            }

        } catch (SQLException sqlEx) {
            throw new DataAccessException("Database error while fetching game", sqlEx);
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
    public void updateGame(GameData updatedGame) throws DataAccessException {
        final String SQL =
                "UPDATE game SET whiteUsername = ?, blackUsername = ?, gameName = ?, gameJSON = ? WHERE gameID = ?";

        String payload = gson.toJson(updatedGame.game());

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setCatalog("chess");

            try (PreparedStatement stmt = conn.prepareStatement(SQL)) {
                stmt.setString(1, updatedGame.whiteUsername());
                stmt.setString(2, updatedGame.blackUsername());
                stmt.setString(3, updatedGame.gameName());
                stmt.setString(4, payload);
                stmt.setInt(5, updatedGame.gameID());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new DataAccessException("No game updated; check game ID.");
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Unable to apply game update", e);
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

    public void testSerialization() {
        var game = new chess.ChessGame();
        var gson = new Gson();

        String json = gson.toJson(game);
        System.out.println("Serialized Game JSON: " + json.substring(0, Math.min(200, json.length())) + "...");

        var restored = gson.fromJson(json, chess.ChessGame.class);
        System.out.println("Deserialized Game Object: " + restored.getClass().getSimpleName());
    }


}
