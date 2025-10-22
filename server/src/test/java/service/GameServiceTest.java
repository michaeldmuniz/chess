package service;

import dataaccess.MemoryDataAccess;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {

    private MemoryDataAccess dao;
    private RegisterService registerService;
    private LoginService loginService;
    private GameService gameService;

    private String authToken;

    @BeforeEach
    void setup() throws Exception {
        dao = new MemoryDataAccess();
        registerService = new RegisterService(dao);
        loginService = new LoginService(dao);
        gameService = new GameService(dao);

        var user = new UserData("gameUser", "password", "email@example.com");
        registerService.register(user);
        var loginResult = loginService.login("gameUser", "password");
        authToken = loginResult.authToken();
    }

    @Test
    void positiveCreateGame() throws Exception {
        var gameID = gameService.createGame(authToken, "NewGame");
        assertTrue(gameID > 0);

        var createdGame = dao.getGame(gameID);
        assertEquals("NewGame", createdGame.gameName());
    }

    @Test
    void negativeCreateGame_invalidToken() {
        assertThrows(Exception.class, () -> gameService.createGame("fake_token", "InvalidGame"));
    }

    @Test
    void positiveListGames() throws Exception {
        gameService.createGame(authToken, "Game1");
        gameService.createGame(authToken, "Game2");

        var games = gameService.listGames(authToken);
        assertEquals(2, games.size());
    }

    @Test
    void negativeListGames_invalidToken() {
        assertThrows(Exception.class, () -> gameService.listGames("bad_token"));
    }

    @Test
    void positiveJoinGame() throws Exception {
        var gameID = gameService.createGame(authToken, "JoinableGame");
        assertDoesNotThrow(() -> gameService.joinGame(authToken, "WHITE", gameID));

        var joined = dao.getGame(gameID);
        assertEquals("gameUser", joined.whiteUsername());
    }

    @Test
    void negativeJoinGame_alreadyTaken() throws Exception {
        var gameID = gameService.createGame(authToken, "TakenGame");
        gameService.joinGame(authToken, "WHITE", gameID);

        // Try joining same color again — should throw
        assertThrows(Exception.class, () -> gameService.joinGame(authToken, "WHITE", gameID));
    }

    @Test
    void negativeJoinGame_invalidGame() {
        assertThrows(Exception.class, () -> gameService.joinGame(authToken, "BLACK", 9999));
    }
}
