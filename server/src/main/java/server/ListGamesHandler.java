package server;

import com.google.gson.Gson;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import service.GameService;
import dataaccess.DataAccessException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.GameData;

public class ListGamesHandler implements Handler {

    private final GameService service;
    private final Gson gson = new Gson();

    public ListGamesHandler(GameService service) {
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

            List<GameData> games = service.listGames(authToken);
            Map<String, Object> response = new HashMap<>();
            response.put("games", games);
            ctx.status(200);
            ctx.result(gson.toJson(response));

        } catch (DataAccessException e) {
            if (e.getMessage().equals("unauthorized")) {
                ctx.status(401);
                ctx.result(gson.toJson(Map.of("message", "Error: unauthorized")));
            } else {
                ctx.status(500);
                ctx.result(gson.toJson(Map.of("message", "Error: " + e.getMessage())));
            }
        } catch (Exception e) {
            ctx.status(500);
            ctx.result(gson.toJson(Map.of("message", "Error: unexpected failure")));
        }
    }
}
