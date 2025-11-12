package client;

import org.junit.jupiter.api.*;
import server.Server;

public class ServerFacadeTests {

    private static Server server;
    private static String BASE_URL;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        BASE_URL = String.format("http://localhost:%d", port);
        System.out.println("Started test HTTP server on " + port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    public void clearWorks() throws Exception {
        var facade = new ServerFacade(BASE_URL);
        facade.clear();
        Assertions.assertTrue(true);
    }
}
