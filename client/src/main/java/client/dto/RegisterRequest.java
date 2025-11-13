package client.dto;


public class RegisterRequest {
    public String username;
    public String password;
    public String email;

    public RegisterRequest(String u, String p, String e) {
        this.username = u;
        this.password = p;
        this.email = e;
    }

}
