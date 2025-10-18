package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.ClearService;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.util.Map;


// Handles HTTP DELETE requests to /db.

public class ClearHandler implements Handler {

    private final ClearService service;
    private final Gson gson = new Gson();

    public ClearHandler(ClearService service) {
        this.service = service;
    }

    @Override
    public void handle(Context ctx) {
        try {
            service.clear();
            ctx.status(200);
            ctx.result(gson.toJson(new Object())); // returns {}
        } catch (DataAccessException e) {
            ctx.status(500);
            ctx.result(gson.toJson(
                    Map.of("message", "Error: " + e.getMessage())
            ));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result(gson.toJson(
                    Map.of("message", "Error: unexpected failure")
            ));
        }
    }
}
