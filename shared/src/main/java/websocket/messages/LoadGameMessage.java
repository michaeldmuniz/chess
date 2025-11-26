package websocket.messages;

import chess.ChessGame;

public class ServerLoadGameMessage extends ServerMessage {

    private ChessGame game;

    public ServerLoadGameMessage(ChessGame game) {
        super(ServerMessageType.LOAD_GAME);
        this.game = game;
    }

    public ChessGame getGame() {
        return game;
    }
}
