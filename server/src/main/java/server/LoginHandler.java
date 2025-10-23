package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import model.AuthData;
import service.LoginService;

import java.util.Map;

public class LoginHandler implements Handler {

    private final LoginService service;
    private final Gson gson = new Gson();

    public LoginHandler(LoginService service) {
        this.service = service;
    }

    @Override
    public void handle(Context ctx) {
        try {
            Map<String, String> request = gson.fromJson(ctx.body(), Map.class);
            String username = request.get("username");
            String password = request.get("password");

            if (username == null || password == null || username.isBlank() || password.isBlank()) {
                ctx.status(400);
                ctx.result(gson.toJson(Map.of("message", "Error: bad request")));
                return;
            }

            AuthData auth = service.login(username, password);

            ctx.status(200);
            ctx.result(gson.toJson(auth));

        } catch (DataAccessException e) {
            String message = e.getMessage();
            if ("unauthorized".equalsIgnoreCase(message)) {
                ctx.status(401);
                ctx.result(gson.toJson(Map.of("message", "Error: unauthorized")));
            } else if ("bad request".equalsIgnoreCase(message)) {
                ctx.status(400);
                ctx.result(gson.toJson(Map.of("message", "Error: bad request")));
            } else {
                ctx.status(500);
                ctx.result(gson.toJson(Map.of("message", "Error: " + message)));
            }

        } catch (Exception e) {
            ctx.status(500);
            ctx.result(gson.toJson(Map.of("message", "Error: unexpected failure")));
        }
    }
}
