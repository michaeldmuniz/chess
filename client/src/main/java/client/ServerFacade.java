package client;

import client.dto.LoginRequest;
import client.dto.LoginResponse;
import client.dto.LogoutResponse;
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
import java.util.Map;

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
    public LoginResponse login(LoginRequest req) throws IOException {
        var conn = makeConnection("/session", "POST");

        try (var out = conn.getOutputStream()) {
            out.write(gson.toJson(req).getBytes());
        }

        conn.connect();
        int status = conn.getResponseCode();

        String raw;
        try (var in = (status == HttpURLConnection.HTTP_OK)
                ? conn.getInputStream()
                : conn.getErrorStream()) {
            raw = new String(in.readAllBytes());
        }

        if (status == HttpURLConnection.HTTP_OK) {
            return gson.fromJson(raw, LoginResponse.class);
        }

        var msg = gson.fromJson(raw, Map.class).get("message").toString();

        throw new IOException(msg);
    }



    public LogoutResponse logout(String authToken) throws IOException {
        if (authToken == null || authToken.isBlank()) {
            throw new IOException("Error: bad request");
        }

        var conn = makeConnection("/session", "DELETE");
        conn.addRequestProperty("Authorization", authToken);

        try {
            conn.connect();
            int status = conn.getResponseCode();

            if (status == HttpURLConnection.HTTP_OK) {
                return new LogoutResponse("ok");
            }

            String body = new String(conn.getErrorStream().readAllBytes());
            var msg = gson.fromJson(body, Map.class).get("message");

            throw new IOException(msg.toString());

        } finally {
            conn.disconnect();
        }
    }

    public Object createGame(Object req) throws IOException {
        // starting to wire this up similar to register/login

        var conn = makeConnection("/game", "POST");
        conn.addRequestProperty("Content-Type", "application/json");

        // write request body
        try (var out = conn.getOutputStream()) {
            var json = gson.toJson(req);
            out.write(json.getBytes());
        }

        // I'm not handling response yet, just returning null for now
        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            // I'll fill this in later with proper error handling
            throw new IOException("game creation failed with status " + status);
        }

        return null; // will replace once I parse responses
    }

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
