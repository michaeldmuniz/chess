package websocket.messages;

public class ServerErrorMessage extends ServerMessage {

    private String errorMessage;

    public ServerErrorMessage(String errorMessage) {
        super(ServerMessageType.ERROR);
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
