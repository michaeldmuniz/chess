package server;

import io.javalin.Javalin;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import dataaccess.MySQLDataAccess;
import dataaccess.DataAccessException;
import service.ClearService;
import service.RegisterService;
import service.LoginService;
import service.LogoutService;
import service.GameService;

public class Server {

    private final Javalin javalin;
    private final DataAccess dao;

    public Server() {
        try {
            // Switch between MySQL or in-memory
            dao = new MySQLDataAccess();
            // dao = new MemoryDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage());
        }

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
        JoinGameHandler joinGameHandler = new JoinGameHandler(gameService);

        javalin.delete("/db", clearHandler);   // Clear the "database"
        javalin.post("/user", registerHandler); // Register new users
        javalin.post("/session", loginHandler); // Log in existing users
        javalin.delete("/session", logoutHandler); // Log out
        javalin.post("/game", gameHandler); // Create game
        javalin.get("/game", listGamesHandler); // List games
        javalin.put("/game", joinGameHandler); // Join game

        WebSocketHandler wsHandler = new WebSocketHandler(dao);
        wsHandler.configure(javalin);
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}