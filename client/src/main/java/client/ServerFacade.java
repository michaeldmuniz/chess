package client;


public class ServerFacade {

    private final String baseUrl;

    private String authToken;

    public ServerFacade(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    public void clear() {
    }

    /* ===== helpers will be added later =====
       - request(method, path, body, respType)
       - read stream utils
       - auth header stuff
    */
}
