package client.ui;

import chess.ChessMove;
import chess.ChessPosition;
import client.websocket.WebSocketClient;
import websocket.commands.UserGameCommand;
import websocket.commands.MakeMoveCommand;

public class GameplayCommands {

    private static String token;
    private static int gameId;

    public static void init(String auth, int id) {
        token = auth;
        gameId = id;
    }

    public static UserGameCommand leave() {
        return new UserGameCommand(
                UserGameCommand.CommandType.LEAVE,
                token, gameId
        );
    }

    public static UserGameCommand resign() {
        return new UserGameCommand(
                UserGameCommand.CommandType.RESIGN,
                token, gameId
        );
    }

    public static MakeMoveCommand move(ChessPosition from, ChessPosition to) {
        ChessMove move = new ChessMove(from, to, null);
        return new MakeMoveCommand(token, gameId, move);
    }

    public static UserGameCommand highlight(ChessPosition from) {
        return new UserGameCommand(
                UserGameCommand.CommandType.HIGHLIGHT,
                token, gameId
        );
    }
    public static void sendHighlight(WebSocketClient ws, String authToken, int gameID, ChessPosition pos) {
        ws.sendHighlight(authToken, gameID, pos);
    }

}
