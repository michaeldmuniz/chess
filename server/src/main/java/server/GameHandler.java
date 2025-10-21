package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import service.GameService;

import java.util.Map;


public class GameHandler implements Handler {

    private final GameService service;
    private final Gson gson = new Gson();

    public GameHandler(GameService service) {
        this.service = service;
    }

    private static class CreateGameRequest {
        String gameName;
    }

    @Override
    public void handle(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            if (authToken == null || authToken.isBlank()) {
                ctx.status(401);
                ctx.result(gson.toJson(Map.of("message", "Error: unauthorized")));
                return;
            }

            var req = gson.fromJson(ctx.body(), CreateGameRequest.class);

            int newId = service.createGame(authToken, (req == null ? null : req.gameName));

            ctx.status(200);
            ctx.result(gson.toJson(Map.of("gameID", newId)));

        } catch (DataAccessException e) {
            switch (e.getMessage()) {
                case "unauthorized" -> ctx.status(401);
                case "bad request"  -> ctx.status(400);
                default             -> ctx.status(500);
            }
            ctx.result(gson.toJson(Map.of("message", "Error: " + e.getMessage())));

        } catch (Exception e) {
            ctx.status(500);
            ctx.result(gson.toJson(Map.of("message", "Error: unexpected failure")));
        }
    }
}
