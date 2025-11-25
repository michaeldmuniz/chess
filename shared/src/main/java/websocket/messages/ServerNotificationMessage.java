package websocket.messages;

public class ServerNotificationMessage extends ServerMessage {

    private String message;

    public ServerNotificationMessage(String message) {
        super(ServerMessageType.NOTIFICATION);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
