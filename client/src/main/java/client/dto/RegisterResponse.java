package client.dto;


public class RegisterResponse {
    public String username;
    public String authToken;

    @Override
    public String toString() {
        return "[RegisterResult username=%s, token=%s]".formatted(username, authToken);
    }
}
