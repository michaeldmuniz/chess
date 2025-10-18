package server;

import io.javalin.*;
import dataaccess.MemoryDataAccess;
import service.ClearService;
import server.ClearHandler;

public class Server {

    private final Javalin javalin;

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        // Create data access and service objects
        var dataAccess = new MemoryDataAccess();
        var clearService = new ClearService(dataAccess);

        // Register endpoints
        javalin.delete("/db", new ClearHandler(clearService));
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
