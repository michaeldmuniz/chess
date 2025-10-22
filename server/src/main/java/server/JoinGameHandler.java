package server;

import com.google.gson.Gson;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import service.GameService;
import dataaccess.DataAccessException;

import java.util.Map;

public class JoinGameHandler implements Handler {

    private final GameService service;
    private final Gson gson = new Gson();

    public JoinGameHandler(GameService service) {
        this.service = service;
    }

    @Override
    public void handle(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            if (authToken == null || authToken.isEmpty()) {
                ctx.status(401);
                ctx.result(gson.toJson(Map.of("message", "Error: unauthorized")));
                return;
            }

            var body = gson.fromJson(ctx.body(), Map.class);
            String playerColor = (String) body.get("playerColor");
            Double gameIDDouble = (Double) body.get("gameID");
            int gameID = gameIDDouble.intValue();

            service.joinGame(authToken, playerColor, gameID);

            ctx.status(200);
            ctx.result(gson.toJson(Map.of()));

        } catch (DataAccessException e) {
            switch (e.getMessage()) {
                case "unauthorized" -> ctx.status(401);
                case "bad request" -> ctx.status(400);
                case "already taken" -> ctx.status(403);
                default -> ctx.status(500);
            }
            ctx.result(gson.toJson(Map.of("message", "Error: " + e.getMessage())));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result(gson.toJson(Map.of("message", "Error: unexpected failure")));
        }
    }
}
