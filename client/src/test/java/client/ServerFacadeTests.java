package client;

import client.dto.LoginRequest;
import client.dto.RegisterRequest;
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
        server = new Server();
        port = server.run(0);
        baseURL = "http://localhost:" + port;
        System.out.println("Started test HTTP server on " + port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    public void sampleTest() {
        Assertions.assertTrue(true);
    }

    @Test
    public void clearWorks() throws Exception {
        var facade = new ServerFacade(baseURL);
        facade.clear();
        Assertions.assertTrue(true);
    }

    @Test
    public void registerSuccess() throws Exception {
        var facade = new ServerFacade(baseURL);

        facade.clear();

        var req = Map.of(
                "username", "mike",
                "password", "pass123",
                "email", "test@something.com"
        );

        Object result = facade.register(req);

        Assertions.assertNotNull(result);
    }

    @Test
    public void loginSuccess() throws Exception {
        var facade = new ServerFacade(baseURL);

        facade.clear();

        facade.register(new RegisterRequest("john", "pass", "j@x.com"));

        var resp = facade.login(new LoginRequest("john", "pass"));

        Assertions.assertNotNull(resp);
        Assertions.assertNotNull(resp.authToken());
        Assertions.assertEquals("john", resp.username());
    }

    @Test
    public void loginBadRequest() throws Exception {
        var facade = new ServerFacade(baseURL);

        facade.clear();

        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.login(new LoginRequest(null, "abc"));
        });

        Assertions.assertEquals("Error: bad request", ex.getMessage());
    }

    @Test
    public void loginUnauthorized() throws Exception {
        var facade = new ServerFacade(baseURL);

        facade.clear();

        // Register real user
        facade.register(new RegisterRequest("mike", "secret", "m@x.com"));

        // Try wrong password
        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.login(new LoginRequest("mike", "wrong"));
        });

        Assertions.assertEquals("Error: unauthorized", ex.getMessage());
    }

    @Test
    public void logoutBadRequest() throws Exception {
        var facade = new ServerFacade(baseURL);

        facade.clear();

        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.logout(null);
        });

        Assertions.assertEquals("Error: bad request", ex.getMessage());
    }

    @Test
    public void logoutUnauthorized() throws Exception {
        var facade = new ServerFacade(baseURL);

        facade.clear();

        // create real user, login so DB has real auth token
        facade.register(new RegisterRequest("sam", "pass", "s@x.com"));
        var loginResp = facade.login(new LoginRequest("sam", "pass"));

        // now attempt logout with *totally fake* token
        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.logout("not-a-real-token-123");
        });

        Assertions.assertEquals("Error: unauthorized", ex.getMessage());
    }

    @Test
    public void createGameSuccess() throws Exception {
        var facade = new ServerFacade(baseURL);

        facade.clear();

        facade.register(new RegisterRequest("amy", "pw", "a@x.com"));
        var login = facade.login(new LoginRequest("amy", "pw"));

        var req = Map.of(
                "authToken", login.authToken(),
                "gameName", "test game"
        );

        Object result = facade.createGame(req);

        Assertions.assertNotNull(result);
        Map<?,?> map = (Map<?,?>) result;
        Assertions.assertTrue(map.containsKey("gameID"));
    }

    @Test
    public void createGameBadRequest() throws Exception {
        var facade = new ServerFacade(baseURL);

        facade.clear();

        facade.register(new RegisterRequest("bob", "123", "b@x.com"));
        var login = facade.login(new LoginRequest("bob", "123"));

        var badReq = Map.of(
                "authToken", login.authToken()
        );

        Exception ex = Assertions.assertThrows(IOException.class, () -> {
            facade.createGame(badReq);
        });

        Assertions.assertEquals("Error: bad request", ex.getMessage());
    }


}
