package dataaccess;

import chess.ChessGame;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class GameDAOTests {

    private MySQLDataAccess dao;

    @BeforeEach
    public void setup() throws DataAccessException {
        dao = new MySQLDataAccess();
        dao.clear();

        dao.createUser(new UserData("testUser", "password", "test@example.com"));
    }

    @Test
    public void createGamePositive() throws DataAccessException {
        // Create a new game
        GameData game = new GameData(0, "testUser", null, "Fun Chess Game", new ChessGame());
        int gameId = dao.createGame(game);

        GameData found = dao.getGame(gameId);
        assertNotNull(found, "Game should be found after being created");
        assertEquals("Fun Chess Game", found.gameName(), "Game name should match the one we gave it");
        assertEquals("testUser", found.whiteUsername(), "White username should be stored correctly");
    }

    @Test
    public void createGameNegative() throws DataAccessException {
        GameData badGame = new GameData(0, "testUser", null, null, new ChessGame());

        assertThrows(DataAccessException.class, () -> dao.createGame(badGame),
                "Should not allow creating a game with missing name");
    }

    @Test
    public void getGameNegative() throws DataAccessException {
        GameData missing = dao.getGame(999);
        assertNull(missing, "Should return null for non-existent game ID");
    }

    @Test
    public void listGamesPositive() throws DataAccessException {
        // Add two games and make sure both show up in the list
        dao.createGame(new GameData(0, "testUser", null, "Game One", new ChessGame()));
        dao.createGame(new GameData(0, "testUser", null, "Game Two", new ChessGame()));

        Collection<GameData> games = dao.listGames();
        assertEquals(2, games.size(), "List should contain exactly two games");
    }

    @Test
    public void updateGamePositive() throws DataAccessException {
        int id = dao.createGame(new GameData(0, "testUser", null, "Original Game", new ChessGame()));

        GameData updated = new GameData(id, "testUser", "otherUser", "Updated Game", new ChessGame());
        dao.updateGame(updated);

        GameData found = dao.getGame(id);
        assertEquals("Updated Game", found.gameName(), "Game name should update correctly");
        assertEquals("otherUser", found.blackUsername(), "Black username should update correctly");
    }

    @Test
    public void updateGameNegative() throws DataAccessException {
        GameData fake = new GameData(999, "a", "b", "Fake", new ChessGame());

        assertThrows(DataAccessException.class, () -> dao.updateGame(fake),
                "Should throw exception for updating non-existent game");
    }
}

