package server;

import io.javalin.Javalin;
import dataaccess.MemoryDataAccess;
import service.ClearService;
import service.RegisterService;
import service.LoginService;
import service.LogoutService;
import service.GameService;


public class Server {

    private final Javalin javalin;
    private final MemoryDataAccess dao = new MemoryDataAccess();

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        ClearService clearService = new ClearService(dao);
        RegisterService registerService = new RegisterService(dao);
        LoginService loginService = new LoginService(dao);
        LogoutService logoutService = new LogoutService(dao);
        GameService gameService = new GameService(dao);

        ClearHandler clearHandler = new ClearHandler(clearService);
        RegisterHandler registerHandler = new RegisterHandler(registerService);
        LoginHandler loginHandler = new LoginHandler(loginService);
        LogoutHandler logoutHandler = new LogoutHandler(logoutService);
        GameHandler gameHandler = new GameHandler(gameService);
        ListGamesHandler listGamesHandler = new ListGamesHandler(gameService);


        javalin.delete("/db", clearHandler);   // Clear the "database"
        javalin.post("/user", registerHandler); // Register new users
        javalin.post("/session", loginHandler); // Log in existing users
        javalin.delete("/session", logoutHandler);
        javalin.post("/game", gameHandler); // Create game
        javalin.get("/game", listGamesHandler);

    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
