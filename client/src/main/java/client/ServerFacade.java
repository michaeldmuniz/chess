package client;

import model.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void register(Object req) throws IOException {}
    public void login(Object req) throws IOException {}
    public void logout(String authToken) throws IOException {}
    public void createGame(Object req) throws IOException {}
    public void listGames(String authToken) throws IOException {}
    public void joinGame(Object req) throws IOException {}
    public void clear() throws IOException {
        var conn = makeConnection("/db", "DELETE");
        try {
            conn.connect();
            var status = conn.getResponseCode();

            if (status != HttpURLConnection.HTTP_OK) {
                System.out.println("⚠Clear failed. Status: " + status);
            } else {
                System.out.println("Database cleared successfully.");
            }
        } finally {
            conn.disconnect();
        }
    }


    private HttpURLConnection makeConnection(String path, String method) throws IOException {
        var url = new URL(serverUrl + path);
        var conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoOutput(true);
        conn.addRequestProperty("Accept", "application/json");
        return conn;
    }
}
