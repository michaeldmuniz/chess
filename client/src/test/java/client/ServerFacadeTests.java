package client;

import org.junit.jupiter.api.*;
import server.Server;
import java.util.Map;

public class ServerFacadeTests {

    private static Server server;
    private static int port;

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);
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
        var facade = new ServerFacade("http://localhost:" + port);
        facade.clear();
        Assertions.assertTrue(true);
    }

    @Test
    public void registerSuccess() throws Exception {
        var facade = new ServerFacade("http://localhost:" + port);

        facade.clear();

        var req = Map.of(
                "username", "mike",
                "password", "pass123",
                "email", "test@something.com"
        );

        Object result = facade.register(req);

        Assertions.assertNotNull(result);
    }
}
