package client;

import model.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public Object register(Object req) throws IOException {
        var conn = makeConnection("/user", "POST");
        conn.setRequestProperty("Content-Type", "application/json");

        writeJsonBody(conn, req);

        var status = conn.getResponseCode();
        var body = readBody(conn);

        if (status >= 200 && status < 300) {
            return gson.fromJson(body, Object.class);
        } else {
            throw new IOException("register failed: " + status + " -> " + body);
        }
    }
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
                System.out.println("Clear failed. Status: " + status);
            } else {
                System.out.println("Database cleared successfully.");
            }
        } finally {
            conn.disconnect();
        }
    }

    private void writeJsonBody(HttpURLConnection conn, Object payload) throws IOException {
        byte[] jsonBytes = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);

        conn.setFixedLengthStreamingMode(jsonBytes.length);

        try (var out = new BufferedOutputStream(conn.getOutputStream())) {
            out.write(jsonBytes);
        }
    }

    private String readBody(HttpURLConnection conn) throws IOException {
        InputStream stream = null;

        try {
            if (conn.getResponseCode() < 400) {
                stream = conn.getInputStream();
            } else {
                stream = conn.getErrorStream();
            }
        } catch (Exception ignored) {
            stream = conn.getErrorStream();
        }

        if (stream == null) return "";

        try (var in = new BufferedInputStream(stream);
             var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             var buffer = new StringWriter()) {

            reader.transferTo(buffer);
            return buffer.toString();
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
