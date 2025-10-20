package server;

import io.javalin.Javalin;
import dataaccess.MemoryDataAccess;
import service.ClearService;
import service.RegisterService;

public class Server {

    private final Javalin javalin;
    private final MemoryDataAccess dao = new MemoryDataAccess();

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        ClearService clearService = new ClearService(dao);
        RegisterService registerService = new RegisterService(dao);

        ClearHandler clearHandler = new ClearHandler(clearService);
        RegisterHandler registerHandler = new RegisterHandler(registerService);

        javalin.delete("/db", clearHandler);   // Clear the "database"
        javalin.post("/user", registerHandler); // Register new users
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
