package client;

import client.dto.*;
import org.junit.jupiter.api.*;
import server.Server;
import java.io.IOException;
import java.util.Map;

public class ServerFacadeTests {

    private static Server server;
    private static int port;
    private static String baseURL;

    @BeforeAll
    public static void init() {
        // Start the server on a random open port before running tests
        server = new Server();
        port = server.run(0);
        baseURL = "http://localhost:" + port;
        System.out.println("Started test HTTP server on " + port);
    }

    @AfterAll
    static void stopServer() {
        // Stop the server after all tests finish
        server.stop();
    }

    @Test
    public void sampleTest() {
        // Simple test to confirm testing works
        Assertions.assertTrue(true);
    }

    @Test
    public void clearWorks() throws Exception {
        // Make sure /db/clear endpoint works without errors
        var facade = new ServerFacade(baseURL);
        facade.clear();
        Assertions.assertTrue(true);
    }

    @Test
    public void registerSuccess() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Build request to register a new user
        var req = new RegisterRequest("mike", "pass123", "test@something.com");

        // Call register
        RegisterResponse result = facade.register(req);
        System.out.println("Register response: " + result);

        // Check the response
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.authToken);
        Assertions.assertEquals("mike", result.username);
    }

    @Test
    public void loginSuccess() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Register a real user first
        facade.register(new RegisterRequest("john", "pass", "j@x.com"));

        // Attempt login
        var resp = facade.login(new LoginRequest("john", "pass"));

        // Print the login response
        System.out.println("Login response: " + resp);

        // Make sure login worked
        Assertions.assertNotNull(resp);
        Assertions.assertNotNull(resp.authToken());
        Assertions.assertEquals("john", resp.username());
    }

    @Test
    public void loginBadRequest() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Login should fail when username is null
        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.login(new LoginRequest(null, "abc"));
        });

        // Print the error
        System.out.println("loginBadRequest exception message: " + ex.getMessage());

        Assertions.assertEquals("Error: bad request", ex.getMessage());
    }

    @Test
    public void loginUnauthorized() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Register a real user
        facade.register(new RegisterRequest("mike", "secret", "m@x.com"));

        // Try logging in with the wrong password
        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.login(new LoginRequest("mike", "wrong"));
        });

        // Print unauthorized error
        System.out.println("Login unauthorized error: " + ex.getMessage());

        Assertions.assertEquals("Error: unauthorized", ex.getMessage());
    }

    @Test
    public void logoutBadRequest() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Logout should fail when token is null
        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.logout(null);
        });

        System.out.println("Logout error message: " + ex.getMessage());
        Assertions.assertEquals("Error: bad request", ex.getMessage());
    }

    @Test
    public void logoutUnauthorized() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Register and login so we have a real authToken
        facade.register(new RegisterRequest("sam", "pass", "s@something.com"));
        var loginResp = facade.login(new LoginRequest("sam", "pass"));

        // Try logging out with a fake token
        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.logout("not-a-real-token-123");
        });

        System.out.println("logoutUnauthorized error message: " + ex.getMessage());
        Assertions.assertEquals("Error: unauthorized", ex.getMessage());
    }

    @Test
    public void createGameSuccess() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Register and login
        facade.register(new RegisterRequest("amy", "pw", "a@x.com"));
        var login = facade.login(new LoginRequest("amy", "pw"));

        // Create game request
        var req = new CreateGameRequest("test game", login.authToken());

        // Call createGame
        CreateGameResponse result = facade.createGame(req);

        System.out.println("Created game with ID: " + result.gameID());

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.gameID() > 0);
    }

    @Test
    public void createGameBadRequest() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        facade.register(new RegisterRequest("bob", "123", "b@x.com"));
        var login = facade.login(new LoginRequest("bob", "123"));

        // Missing game name should result in bad request
        var badReq = new CreateGameRequest(null, login.authToken());

        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.createGame(badReq);
        });

        System.out.println("Received error: " + ex.getMessage());
        Assertions.assertEquals("Error: bad request", ex.getMessage());
    }

    @Test
    public void listGamesSuccess() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Register + login
        facade.register(new RegisterRequest("sam", "pass123", "s@x.com"));
        var loginResp = facade.login(new LoginRequest("sam", "pass123"));

        // Create two games
        facade.createGame(new CreateGameRequest("game1", loginResp.authToken()));
        facade.createGame(new CreateGameRequest("game2", loginResp.authToken()));

        // Request list of games
        var listReq = new ListGamesRequest(loginResp.authToken());
        var gamesResp = facade.listGames(listReq);

        System.out.println("Games returned: " + gamesResp.games());

        Assertions.assertNotNull(gamesResp);
        Assertions.assertNotNull(gamesResp.games());
        Assertions.assertTrue(gamesResp.games().size() >= 2);
    }

    @Test
    public void listGamesUnauthorized() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Invalid token should cause unauthorized error
        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.listGames(new ListGamesRequest("not-a-real-token"));
        });

        System.out.println("Received error: " + ex.getMessage());
        Assertions.assertEquals("Error: unauthorized", ex.getMessage());
    }

    @Test
    public void listGamesEmptyList() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Register + login, but create no games
        facade.register(new RegisterRequest("amy", "pw", "a@x.com"));
        var loginResp = facade.login(new LoginRequest("amy", "pw"));

        // Request game list
        var list = facade.listGames(new ListGamesRequest(loginResp.authToken()));

        System.out.println("Games returned (should be empty): " + list.games());

        Assertions.assertNotNull(list);
        Assertions.assertNotNull(list.games());
        Assertions.assertEquals(0, list.games().size());
    }

    @Test
    public void joinGameSuccess() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Register and login
        var reg = facade.register(new RegisterRequest("mike", "pass", "m@x.com"));
        var login = facade.login(new LoginRequest("mike", "pass"));

        // Create game
        var gameResp = facade.createGame(new CreateGameRequest("testGame", login.authToken()));
        int gameID = gameResp.gameID();

        // Join the game
        var joinReq = new JoinGameRequest("WHITE", gameID);
        var result = facade.joinGame(joinReq, login.authToken());

        System.out.println("Joined game successfully");

        Assertions.assertNotNull(result);
    }

    @Test
    public void joinGameBadRequest() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Register + login
        facade.register(new RegisterRequest("lee", "pw", "l@x.com"));
        var loginResp = facade.login(new LoginRequest("lee", "pw"));

        // Invalid game ID should cause bad request
        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.joinGame(new JoinGameRequest("WHITE", 0), loginResp.authToken());
        });
        System.out.println("joinGameBadRequest error: " + ex.getMessage());

        Assertions.assertEquals("Error: bad request", ex.getMessage());
    }

    @Test
    public void joinGameUnauthorized() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();

        // Using a fake token should fail
        var req = new JoinGameRequest("WHITE", 1);

        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.joinGame(req, "not-a-token");
        });
        System.out.println("joinGameUnauthorized error message: " + ex.getMessage());

        Assertions.assertEquals("Error: unauthorized", ex.getMessage());
    }


}
