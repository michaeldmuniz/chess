package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import websocket.commands.UserGameCommand;
import websocket.commands.MakeMoveCommand;
import websocket.messages.*;

public class WebSocketHandler {

    private final WebSocketConnectionManager manager = new WebSocketConnectionManager();
    private final DataAccess dao;
    private final Gson gson = new Gson();

    public WebSocketHandler(DataAccess dao) {
        this.dao = dao;
    }

    public void configure(Javalin app) {
        app.ws("/ws", ws -> {

            ws.onConnect(ctx -> {
                System.out.println("WS connected: " + ctx.sessionId());
            });

            ws.onClose(ctx -> {
                manager.removeSession(ctx);
            });

            ws.onError(ctx -> {
                manager.removeSession(ctx);
            });

            ws.onMessage(ctx -> {
                String json = ctx.message();
                UserGameCommand baseCmd = gson.fromJson(json, UserGameCommand.class);
                routeCommand(ctx, baseCmd, json);
            });
        });
    }

    private void routeCommand(WsContext ctx, UserGameCommand cmd, String rawJson) {

        switch (cmd.getCommandType()) {

            case CONNECT -> handleConnect(ctx, cmd);

            case MAKE_MOVE -> {
                MakeMoveCommand moveCmd = gson.fromJson(rawJson, MakeMoveCommand.class);
                handleMakeMove(ctx, moveCmd);
            }

            case LEAVE -> handleLeave(ctx, cmd);

            case RESIGN -> handleResign(ctx, cmd);
        }
    }

    private void handleConnect(WsContext ctx, UserGameCommand cmd) {
        try {
            var auth = dao.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: invalid auth token");
                return;
            }

            var game = dao.getGame(cmd.getGameID());
            if (game == null) {
                sendError(ctx, "Error: invalid game ID");
                return;
            }

            String username = auth.username();
            String role;

            if (game.whiteUsername() != null && game.whiteUsername().equals(username)) {
                role = "white";
            } else if (game.blackUsername() != null && game.blackUsername().equals(username)) {
                role = "black";
            } else {
                role = "observer";
            }

            // store session
            manager.addSession(ctx, username, cmd.getGameID(), role);

            // send LOAD_GAME to this session
            var load = new LoadGameMessage(game);
            ctx.send(gson.toJson(load));

            // notify others
            var note = new NotificationMessage(username + " connected as " + role);
            manager.broadcastToGameExcept(ctx, cmd.getGameID(), note);

        } catch (Exception ex) {
            sendError(ctx, "Error: " + ex.getMessage());
        }
    }


    private void handleMakeMove(WsContext ctx, MakeMoveCommand cmd) {
    }

    private void handleLeave(WsContext ctx, UserGameCommand cmd) {
    }

    private void handleResign(WsContext ctx, UserGameCommand cmd) {
    }

    private void sendError(WsContext ctx, String msg) {
        ErrorMessage error = new ErrorMessage(msg);
        ctx.send(gson.toJson(error));
    }

}
