package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import model.AuthData;
import model.UserData;
import service.RegisterService;

import java.util.Map;


public class RegisterHandler implements Handler {

    private final RegisterService service;
    private final Gson gson = new Gson();

    public RegisterHandler(RegisterService service) {
        this.service = service;
    }

    @Override
    public void handle(Context ctx) {
        try {
            var request = gson.fromJson(ctx.body(), UserData.class);

            AuthData auth = service.register(request);

            ctx.status(200);
            ctx.result(gson.toJson(Map.of(
                    "username", auth.username(),
                    "authToken", auth.authToken()
            )));

        } catch (DataAccessException e) {
            if (e.getMessage().equals("bad request")) {
                ctx.status(400);
            } else if (e.getMessage().equals("already taken")) {
                ctx.status(403);
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
