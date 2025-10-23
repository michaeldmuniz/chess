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

            AuthData auth = service.login(username, password);

            ctx.status(200);
            ctx.result(gson.toJson(auth));

        } catch (DataAccessException e) {
            // Handle known problems
            if (e.getMessage().equals("unauthorized")) {
                ctx.status(400);
            } else {
                ctx.status(500);
            }
            ctx.result(gson.toJson(Map.of("message", "Error: " + e.getMessage())));

        } catch (Exception e) {
            // Handle unexpected problems
            ctx.status(500);
            ctx.result(gson.toJson(Map.of("message", "Error: unexpected failure")));
        }
    }
}
