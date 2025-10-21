package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import service.LogoutService;

import java.util.Map;


public class LogoutHandler implements Handler {

    private final LogoutService service;
    private final Gson gson = new Gson();

    public LogoutHandler(LogoutService service) {
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

            service.logout(authToken);

            ctx.status(200);
            ctx.result(gson.toJson(new Object()));

        } catch (DataAccessException e) {
            if (e.getMessage().equals("unauthorized")) {
                ctx.status(401);
            } else {
                ctx.status(500);
            }
            ctx.result(gson.toJson(Map.of("message", "Error: " + e.getMessage())));

        } catch (Exception e) {
            ctx.status(500);
            ctx.result(gson.toJson(Map.of("message", "Error: unexpected failure")));
        }
    }
}
