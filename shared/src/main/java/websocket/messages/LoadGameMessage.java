package websocket.messages;

public class LoadGameMessage extends ServerMessage {

    private final Object game;

    public LoadGameMessage(Object game) {
        super(ServerMessageType.LOAD_GAME);
        this.game = game;
    }

    public Object getGame() {
        return game;
    }
}
